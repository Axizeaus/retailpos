package com.retailpos.dto;

import com.retailpos.enums.InventoryMovementType;
import java.time.Instant;

public record InventoryMovementResponse(
        Long id,
        Long productId,
        String productName,
        InventoryMovementType movementType,
        int quantityChange,
        int quantityBefore,
        int quantityAfter,
        String note,
        Instant createdAt
) {
}