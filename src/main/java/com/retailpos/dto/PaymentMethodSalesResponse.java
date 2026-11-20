package com.retailpos.dto;

import com.retailpos.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentMethodSalesResponse(
        PaymentMethod paymentMethod,
        Long transactionCount,
        BigDecimal totalSales
) {
}