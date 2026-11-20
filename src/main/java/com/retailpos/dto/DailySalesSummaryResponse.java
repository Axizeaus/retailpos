package com.retailpos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailySalesSummaryResponse(
        LocalDate date,
        Long transactionCount,
        BigDecimal totalSales,
        BigDecimal averageTransactionValue,
        BigDecimal totalCashTendered,
        BigDecimal totalChangeGiven,
        List<PaymentMethodSalesResponse> paymentMethods
) {
}