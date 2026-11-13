package com.retailpos.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long productId,
        String productName,
        String productSku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}