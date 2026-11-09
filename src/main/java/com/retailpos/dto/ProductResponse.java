package com.retailpos.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        int stockQuantity,
        boolean active
) {
}
