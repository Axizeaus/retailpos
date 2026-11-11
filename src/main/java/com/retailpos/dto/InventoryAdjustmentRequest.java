package com.retailpos.dto;

import com.retailpos.enums.InventoryMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(

        @NotNull
        InventoryMovementType movementType,

        @NotNull
        Integer quantityChange,

        @Size(max = 1000)
        String note

) {
}