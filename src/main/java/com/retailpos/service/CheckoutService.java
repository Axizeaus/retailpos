package com.retailpos.service;

import com.retailpos.dto.CheckoutItemRequest;
import com.retailpos.dto.CheckoutRequest;
import com.retailpos.dto.SaleItemResponse;
import com.retailpos.dto.SaleResponse;
import com.retailpos.entity.InventoryMovement;
import com.retailpos.entity.Product;
import com.retailpos.entity.Sale;
import com.retailpos.entity.SaleItem;
import com.retailpos.enums.InventoryMovementType;
import com.retailpos.enums.PaymentMethod;
import com.retailpos.exception.InsufficientStockException;
import com.retailpos.exception.InvalidCheckoutException;
import com.retailpos.exception.ProductNotFoundException;
import com.retailpos.repository.InventoryMovementRepository;
import com.retailpos.repository.ProductRepository;
import com.retailpos.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional
    public SaleResponse checkout(CheckoutRequest request) {

        validatePaymentMethod(request);
        validateUniqueProducts(request.items());

        List<CheckoutItemRequest> sortedItems = request.items().stream()
                .sorted((a, b) -> a.productId().compareTo(b.productId()))
                .toList();

        Map<Long, Product> productsById = new HashMap<>();

        for (CheckoutItemRequest item : sortedItems) {
            Product product = productRepository.findByIdForUpdate(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));

            if (!product.isActive()) {
                throw new ProductNotFoundException(item.productId());
            }

            if (product.getStockQuantity() < item.quantity()) {
                throw new InsufficientStockException(item.productId());
            }

            productsById.put(product.getId(), product);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CheckoutItemRequest item : request.items()) {
            Product product = productsById.get(item.productId());

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(item.quantity()));

            totalAmount = totalAmount.add(lineTotal);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCheckoutException(
                    "Sale total must be greater than zero."
            );
        }

        BigDecimal cashTendered = request.cashTendered();

        if (cashTendered.compareTo(totalAmount) < 0) {
            throw new InvalidCheckoutException(
                    "Cash tendered must be greater than or equal to the sale total."
            );
        }

        BigDecimal changeDue = cashTendered.subtract(totalAmount);
        String receiptNumber = generateReceiptNumber();

        Sale sale = new Sale(
                receiptNumber,
                totalAmount,
                cashTendered,
                changeDue,
                request.paymentMethod()
        );

        List<SaleItemResponse> itemResponses = new ArrayList<>();

        for (CheckoutItemRequest item : request.items()) {
            Product product = productsById.get(item.productId());

            int quantity = item.quantity();
            int quantityBefore = product.getStockQuantity();
            int quantityAfter = quantityBefore - quantity;

            product.setStockQuantity(quantityAfter);

            SaleItem saleItem = new SaleItem(sale, product, quantity);
            sale.addItem(saleItem);

            InventoryMovement movement = new InventoryMovement(
                    product,
                    InventoryMovementType.SALE,
                    -quantity,
                    quantityBefore,
                    quantityAfter,
                    "Sale receipt: " + receiptNumber
            );

            inventoryMovementRepository.save(movement);

            itemResponses.add(new SaleItemResponse(
                    product.getId(),
                    saleItem.getProductName(),
                    saleItem.getProductSku(),
                    saleItem.getUnitPrice(),
                    saleItem.getQuantity(),
                    saleItem.getLineTotal()
            ));
        }

        Sale savedSale = saleRepository.save(sale);

        return new SaleResponse(
                savedSale.getId(),
                savedSale.getReceiptNumber(),
                savedSale.getTotalAmount(),
                savedSale.getCashTendered(),
                savedSale.getChangeDue(),
                savedSale.getPaymentMethod(),
                savedSale.getCreatedAt(),
                itemResponses
        );
    }

    private void validatePaymentMethod(CheckoutRequest request) {
        if (request.paymentMethod() != PaymentMethod.CASH) {
            throw new InvalidCheckoutException(
                    "Only cash payments are currently supported."
            );
        }
    }

    private void validateUniqueProducts(List<CheckoutItemRequest> items) {
        Set<Long> productIds = new HashSet<>();

        for (CheckoutItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new InvalidCheckoutException(
                        "Duplicate products are not allowed in a checkout."
                );
            }
        }
    }

    private String generateReceiptNumber() {
        return "RCP-" + UUID.randomUUID().toString().toUpperCase();
    }
}