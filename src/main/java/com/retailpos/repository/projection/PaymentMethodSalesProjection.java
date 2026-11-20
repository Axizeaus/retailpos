
package com.retailpos.repository.projection;

import com.retailpos.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentMethodSalesProjection {

    PaymentMethod getPaymentMethod();

    Long getTransactionCount();

    BigDecimal getTotalSales();
}
