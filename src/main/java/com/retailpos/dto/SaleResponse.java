package com.retailpos.dto;

import com.retailpos.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        String receiptNumber,
        BigDecimal totalAmount,
        BigDecimal cashTendered,
        BigDecimal changeDue,
        PaymentMethod paymentMethod,
        Instant createdAt,
        List<SaleItemResponse> items
) {
}