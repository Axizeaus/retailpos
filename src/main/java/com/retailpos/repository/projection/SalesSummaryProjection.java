package com.retailpos.repository.projection;

import java.math.BigDecimal;

public interface SalesSummaryProjection{
    Long getTransactionCount();
    BigDecimal getTotalSales();
    BigDecimal getTotalCashTendered();
    BigDecimal getTotalChangeGiven();
}