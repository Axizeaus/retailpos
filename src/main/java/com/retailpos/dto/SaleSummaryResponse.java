package com.retailpos.dto;

import com.retailpos.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleSummaryResponse(
        Long id,
        String receiptNumber,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        Instant createdAt
) {
}