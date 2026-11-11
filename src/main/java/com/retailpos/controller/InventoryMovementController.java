package com.retailpos.controller;

import com.retailpos.dto.InventoryAdjustmentRequest;
import com.retailpos.dto.InventoryMovementResponse;
import com.retailpos.service.InventoryMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}")
@RequiredArgsConstructor
public class InventoryMovementController {

    private final InventoryMovementService inventoryMovementService;

    @GetMapping("/inventory-movements")
    public Page<InventoryMovementResponse> getProductMovements(
            @PathVariable Long productId,
            Pageable pageable
    ) {
        return inventoryMovementService.getProductMovements(productId, pageable);
    }

    @PostMapping("/inventory-adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementResponse adjustStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryAdjustmentRequest request
    ) {
        return inventoryMovementService.adjustStock(productId, request);
    }

}