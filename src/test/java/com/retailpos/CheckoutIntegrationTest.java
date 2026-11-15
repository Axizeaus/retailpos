
package com.retailpos;

import com.jayway.jsonpath.JsonPath;
import com.retailpos.entity.InventoryMovement;
import com.retailpos.entity.Sale;
import com.retailpos.enums.InventoryMovementType;
import com.retailpos.repository.InventoryMovementRepository;
import com.retailpos.repository.ProductRepository;
import com.retailpos.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CheckoutIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Test
    void shouldCompleteCashCheckoutAndUpdateInventory() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        String checkoutBody = """
                {
                    "paymentMethod": "CASH",
                    "cashTendered": 50.00,
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 3
                        }
                    ]
                }
                """.formatted(testProduct.productId());

        MvcResult checkoutResult = mockMvc.perform(
                        post("/api/checkout")
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").isNotEmpty())
                .andExpect(jsonPath("$.totalAmount").value(37.50))
                .andExpect(jsonPath("$.cashTendered").value(50.00))
                .andExpect(jsonPath("$.changeDue").value(12.50))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andReturn();

        Number saleIdValue = JsonPath.read(
                checkoutResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long saleId = saleIdValue.longValue();

        var product = productRepository
                .findByIdAndShopId(
                        testProduct.productId(),
                        testProduct.shopId()
                )
                .orElseThrow();

        assertThat(product.getStockQuantity()).isEqualTo(17);

        Sale sale = saleRepository.findById(saleId).orElseThrow();

        assertThat(sale.getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("37.50"));

        assertThat(sale.getCashTendered())
                .isEqualByComparingTo(new BigDecimal("50.00"));

        var movements = inventoryMovementRepository
                .findAllByProductIdAndProductShopIdOrderByCreatedAtDesc(
                        testProduct.productId(),
                        testProduct.shopId(),
                        org.springframework.data.domain.PageRequest.of(0, 10)
                );

        assertThat(movements.getContent()).hasSize(1);

        InventoryMovement movement = movements.getContent().getFirst();

        assertThat(movement.getMovementType())
                .isEqualTo(InventoryMovementType.SALE);

        assertThat(movement.getQuantityChange()).isEqualTo(-3);
        assertThat(movement.getQuantityBefore()).isEqualTo(20);
        assertThat(movement.getQuantityAfter()).isEqualTo(17);
    }

    @Test
    void shouldRejectCheckoutWhenStockIsInsufficient() throws Exception {
        TestProduct testProduct = createTestProduct(2);



        String checkoutBody = """
                {
                    "paymentMethod": "CASH",
                    "cashTendered": 50.00,
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 3
                        }
                    ]
                }
                """.formatted(testProduct.productId());

        long salesBefore = saleRepository.count();

        mockMvc.perform(
                        post("/api/checkout")
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody)
                )
                .andExpect(status().isConflict());

        var product = productRepository
                .findByIdAndShopId(
                        testProduct.productId(),
                        testProduct.shopId()
                )
                .orElseThrow();

        assertThat(product.getStockQuantity()).isEqualTo(2);

        var movements = inventoryMovementRepository
                .findAllByProductIdAndProductShopIdOrderByCreatedAtDesc(
                        testProduct.productId(),
                        testProduct.shopId(),
                        org.springframework.data.domain.PageRequest.of(0, 10)
                );

        assertThat(movements.getContent()).isEmpty();

        assertThat(saleRepository.count()).isEqualTo(salesBefore);
    }

    @Test
    void shouldRollbackEntireCheckoutWhenOneProductHasInsufficientStock()
            throws Exception {

        TestProduct firstProduct = createTestProduct(10);
        TestProduct secondProduct = createAdditionalProduct(firstProduct, 1);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 100.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
                    },
                    {
                        "productId": %d,
                        "quantity": 3
                    }
                ]
            }
            """.formatted(
                firstProduct.productId(),
                secondProduct.productId()
        );

        long salesBefore = saleRepository.count();

        mockMvc.perform(
                        post("/api/checkout")
                                .header("Authorization", firstProduct.authorization())
                                .header("X-Shop-Id", firstProduct.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody)
                )
                .andExpect(status().isConflict());

        var firstProductAfter = productRepository
                .findByIdAndShopId(
                        firstProduct.productId(),
                        firstProduct.shopId()
                )
                .orElseThrow();

        var secondProductAfter = productRepository
                .findByIdAndShopId(
                        secondProduct.productId(),
                        secondProduct.shopId()
                )
                .orElseThrow();

        assertThat(firstProductAfter.getStockQuantity()).isEqualTo(10);
        assertThat(secondProductAfter.getStockQuantity()).isEqualTo(1);

        var firstProductMovements = inventoryMovementRepository
                .findAllByProductIdAndProductShopIdOrderByCreatedAtDesc(
                        firstProduct.productId(),
                        firstProduct.shopId(),
                        org.springframework.data.domain.PageRequest.of(0, 10)
                );

        var secondProductMovements = inventoryMovementRepository
                .findAllByProductIdAndProductShopIdOrderByCreatedAtDesc(
                        secondProduct.productId(),
                        secondProduct.shopId(),
                        org.springframework.data.domain.PageRequest.of(0, 10)
                );

        assertThat(firstProductMovements.getContent()).isEmpty();
        assertThat(secondProductMovements.getContent()).isEmpty();

        assertThat(saleRepository.count()).isEqualTo(salesBefore);
    }

    private TestProduct createTestProduct(int initialStock) throws Exception {
        String email = "checkout-" + UUID.randomUUID() + "@example.com";

        String registrationBody = """
                {
                    "email": "%s",
                    "password": "StrongPassword123!",
                    "shopName": "Checkout Test Shop"
                }
                """.formatted(email);

        MvcResult registrationResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String registrationResponse =
                registrationResult.getResponse().getContentAsString();

        String accessToken = JsonPath.read(
                registrationResponse, "$.accessToken"
        );

        Number shopIdValue = JsonPath.read(
                registrationResponse, "$.memberships[0].shopId"
        );

        Long shopId = shopIdValue.longValue();

        String authorization = "Bearer " + accessToken;

        String categoryBody = """
                {
                    "name": "Beverages",
                    "description": "Drinks"
                }
                """;

        MvcResult categoryResult = mockMvc.perform(
                        post("/api/categories")
                                .header("Authorization", authorization)
                                .header("X-Shop-Id", shopId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(categoryBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number categoryIdValue = JsonPath.read(
                categoryResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long categoryId = categoryIdValue.longValue();

        String sku = "WATER-" + UUID.randomUUID();

        String productBody = """
                {
                    "name": "Mineral Water",
                    "sku": "%s",
                    "price": 12.50,
                    "stockQuantity": %d,
                    "categoryId": %d,
                    "barcode": null
                }
                """.formatted(sku, initialStock, categoryId);

        MvcResult productResult = mockMvc.perform(
                        post("/api/products")
                                .header("Authorization", authorization)
                                .header("X-Shop-Id", shopId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number productIdValue = JsonPath.read(
                productResult.getResponse().getContentAsString(),
                "$.id"
        );

        return new TestProduct(
                productIdValue.longValue(),
                shopId,
                authorization
        );
    }

    private TestProduct createAdditionalProduct(
            TestProduct existingProduct,
            int initialStock
    ) throws Exception {

        String categoryBody = """
            {
                "name": "Additional Beverages",
                "description": "Additional drinks"
            }
            """;

        MvcResult categoryResult = mockMvc.perform(
                        post("/api/categories")
                                .header("Authorization", existingProduct.authorization())
                                .header("X-Shop-Id", existingProduct.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(categoryBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number categoryIdValue = JsonPath.read(
                categoryResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long categoryId = categoryIdValue.longValue();

        String sku = "JUICE-" + UUID.randomUUID();

        String productBody = """
            {
                "name": "Orange Juice",
                "sku": "%s",
                "price": 15.00,
                "stockQuantity": %d,
                "categoryId": %d,
                "barcode": null
            }
            """.formatted(sku, initialStock, categoryId);

        MvcResult productResult = mockMvc.perform(
                        post("/api/products")
                                .header("Authorization", existingProduct.authorization())
                                .header("X-Shop-Id", existingProduct.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number productIdValue = JsonPath.read(
                productResult.getResponse().getContentAsString(),
                "$.id"
        );

        return new TestProduct(
                productIdValue.longValue(),
                existingProduct.shopId(),
                existingProduct.authorization()
        );
    }

    private record TestProduct(
            Long productId,
            Long shopId,
            String authorization
    ) {
    }
}
