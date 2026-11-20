
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

    @Test
    void shouldReturnSalesHistory() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 50.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
                    }
                ]
            }
            """.formatted(testProduct.productId());

        mockMvc.perform(
                        post("/api/checkout")
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/sales")
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].receiptNumber").isNotEmpty())
                .andExpect(jsonPath("$.content[0].totalAmount").value(25.00))
                .andExpect(jsonPath("$.content[0].paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.content[0].items").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldFilterSalesByReceiptNumber() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 50.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
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
                .andReturn();

        String receiptNumber = JsonPath.read(
                checkoutResult.getResponse().getContentAsString(),
                "$.receiptNumber"
        );

        mockMvc.perform(
                        get("/api/sales")
                                .param("receiptNumber", receiptNumber.substring(4, 12))
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].receiptNumber").value(receiptNumber))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldFilterSalesByDateRange() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 50.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
                    }
                ]
            }
            """.formatted(testProduct.productId());

        mockMvc.perform(
                        post("/api/checkout")
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody)
                )
                .andExpect(status().isCreated());

        // cuz Asia/yangon is what's currently set up for the time zone rn.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Yangon"));

        mockMvc.perform(
                        get("/api/sales")
                                .param("startDate", today.toString())
                                .param("endDate", today.toString())
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldReturnSaleDetailsById() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 50.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
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
                .andReturn();

        Number saleId = JsonPath.read(
                checkoutResult.getResponse().getContentAsString(),
                "$.id"
        );

        mockMvc.perform(
                        get("/api/sales/{id}", saleId.longValue())
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saleId.intValue()))
                .andExpect(jsonPath("$.totalAmount").value(25.00))
                .andExpect(jsonPath("$.cashTendered").value(50.00))
                .andExpect(jsonPath("$.changeDue").value(25.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId")
                        .value(testProduct.productId()))
                .andExpect(jsonPath("$.items[0].productName")
                        .value("Mineral Water"))
                .andExpect(jsonPath("$.items[0].productSku").isNotEmpty())
                .andExpect(jsonPath("$.items[0].unitPrice").value(12.50))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(25.00));
    }

    @Test
    void shouldReturnSaleDetailsByReceiptNumber() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 50.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
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
                .andReturn();

        String receiptNumber = JsonPath.read(
                checkoutResult.getResponse().getContentAsString(),
                "$.receiptNumber"
        );

        mockMvc.perform(
                        get("/api/sales/receipt/{receiptNumber}", receiptNumber)
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value(receiptNumber))
                .andExpect(jsonPath("$.totalAmount").value(25.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void shouldNotAllowAnotherShopToAccessSale() throws Exception {
        TestProduct firstShop = createTestProduct(20);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 50.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
                    }
                ]
            }
            """.formatted(firstShop.productId());

        MvcResult checkoutResult = mockMvc.perform(
                        post("/api/checkout")
                                .header("Authorization", firstShop.authorization())
                                .header("X-Shop-Id", firstShop.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = checkoutResult.getResponse().getContentAsString();

        Number saleId = JsonPath.read(responseBody, "$.id");
        String receiptNumber = JsonPath.read(responseBody, "$.receiptNumber");


        TestProduct secondShop = createTestProduct(20);

        mockMvc.perform(
                        get("/api/sales/{id}", saleId.longValue())
                                .header("Authorization", secondShop.authorization())
                                .header("X-Shop-Id", secondShop.shopId())
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        get("/api/sales")
                                .header("Authorization", secondShop.authorization())
                                .header("X-Shop-Id", secondShop.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mockMvc.perform(
                        get("/api/sales/receipt/{receiptNumber}", receiptNumber)
                                .header("Authorization", secondShop.authorization())
                                .header("X-Shop-Id", secondShop.shopId())
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenSaleDoesNotExist() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        mockMvc.perform(
                        get("/api/sales/{id}", Long.MAX_VALUE)
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        get("/api/sales/receipt/{receiptNumber}", "RCP-NONEXISTENT")
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnDailySalesSummary() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 50.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
                    }
                ]
            }
            """.formatted(testProduct.productId());

        mockMvc.perform(
                        post("/api/checkout")
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody)
                )
                .andExpect(status().isCreated());

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Yangon"));

        mockMvc.perform(
                        get("/api/sales/summary/daily")
                                .param("date", today.toString())
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.transactionCount").value(1))
                .andExpect(jsonPath("$.totalSales").value(25.00))
                .andExpect(jsonPath("$.averageTransactionValue").value(25.00))
                .andExpect(jsonPath("$.totalCashTendered").value(50.00))
                .andExpect(jsonPath("$.totalChangeGiven").value(25.00))
                .andExpect(jsonPath("$.paymentMethods.length()").value(1))
                .andExpect(jsonPath("$.paymentMethods[0].paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.paymentMethods[0].transactionCount").value(1))
                .andExpect(jsonPath("$.paymentMethods[0].totalSales").value(25.00));
    }

    @Test
    void shouldReturnZeroSummaryWhenThereAreNoSales() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Yangon"))
                .minusDays(1);

        mockMvc.perform(
                        get("/api/sales/summary/daily")
                                .param("date", yesterday.toString())
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(yesterday.toString()))
                .andExpect(jsonPath("$.transactionCount").value(0))
                .andExpect(jsonPath("$.totalSales").value(0))
                .andExpect(jsonPath("$.averageTransactionValue").value(0))
                .andExpect(jsonPath("$.totalCashTendered").value(0))
                .andExpect(jsonPath("$.totalChangeGiven").value(0))
                .andExpect(jsonPath("$.paymentMethods.length()").value(0));
    }

    @Test
    void shouldNotIncludeAnotherShopsSalesInDailySummary() throws Exception {
        TestProduct firstShop = createTestProduct(20);

        String checkoutBody = """
            {
                "paymentMethod": "CASH",
                "cashTendered": 50.00,
                "items": [
                    {
                        "productId": %d,
                        "quantity": 2
                    }
                ]
            }
            """.formatted(firstShop.productId());

        mockMvc.perform(
                        post("/api/checkout")
                                .header("Authorization", firstShop.authorization())
                                .header("X-Shop-Id", firstShop.shopId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody)
                )
                .andExpect(status().isCreated());

        TestProduct secondShop = createTestProduct(20);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Yangon"));

        mockMvc.perform(
                        get("/api/sales/summary/daily")
                                .param("date", today.toString())
                                .header("Authorization", secondShop.authorization())
                                .header("X-Shop-Id", secondShop.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(0))
                .andExpect(jsonPath("$.totalSales").value(0))
                .andExpect(jsonPath("$.paymentMethods.length()").value(0));
    }

    @Test
    void shouldUseCurrentBusinessDateWhenDateIsNotProvided() throws Exception {
        TestProduct testProduct = createTestProduct(20);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Yangon"));

        mockMvc.perform(
                        get("/api/sales/summary/daily")
                                .header("Authorization", testProduct.authorization())
                                .header("X-Shop-Id", testProduct.shopId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.transactionCount").value(0))
                .andExpect(jsonPath("$.totalSales").value(0));
    }

    private TestProduct createTestProduct(int initialStock) throws Exception {
        String email = "checkout-" + UUID.randomUUID() + "@example.com";

        String registrationBody = """
            {
                "name": "Checkout Test User",
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
