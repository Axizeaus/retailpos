package com.retailpos.dto;

import com.retailpos.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutRequest(

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Cash tendered is required")
        @DecimalMin(value = "0.01", message = "Cash tendered must be greater than zero")
        BigDecimal cashTendered,

        @NotEmpty(message = "A sale must contain at least one item")
        List<@NotNull @Valid CheckoutItemRequest> items

) {
}