package com.retailpos.service;

import com.retailpos.dto.InventoryAdjustmentRequest;
import com.retailpos.dto.InventoryMovementResponse;
import com.retailpos.entity.InventoryMovement;
import com.retailpos.entity.Product;
import com.retailpos.exception.InsufficientStockException;
import com.retailpos.exception.InvalidInventoryAdjustmentException;
import com.retailpos.exception.ProductNotFoundException;
import com.retailpos.repository.InventoryMovementRepository;
import com.retailpos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryMovementService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional
    public InventoryMovementResponse adjustStock(
            Long productId,
            InventoryAdjustmentRequest request
    ) {
        // this is pessimistic locking btw.
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!product.isActive()) {
            throw new ProductNotFoundException(productId);
        }

        validateAdjustment(request);

        int quantityBefore = product.getStockQuantity();
        int quantityAfter = quantityBefore + request.quantityChange();

        if (quantityAfter < 0) {
            throw new InsufficientStockException(productId);
        }

        product.setStockQuantity(quantityAfter);

        InventoryMovement movement = new InventoryMovement(
                product,
                request.movementType(),
                request.quantityChange(),
                quantityBefore,
                quantityAfter,
                request.note()
        );

        InventoryMovement savedMovement = inventoryMovementRepository.save(movement);

        return toInventoryMovementResponse(savedMovement);
    }

    public Page<InventoryMovementResponse> getProductMovements(
            Long productId,
            Pageable pageable
    ) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        return inventoryMovementRepository
                .findAllByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toInventoryMovementResponse);
    }

    private void validateAdjustment(InventoryAdjustmentRequest request) {
        int quantityChange = request.quantityChange();

        if (quantityChange == 0) {
            throw new InvalidInventoryAdjustmentException(
                    "Quantity change cannot be zero."
            );
        }

        switch (request.movementType()) {
            case STOCK_RECEIVED -> {
                if (quantityChange < 0) {
                    throw new InvalidInventoryAdjustmentException(
                            "Stock received must have a positive quantity."
                    );
                }
            }
            case DAMAGE -> {
                if (quantityChange > 0) {
                    throw new InvalidInventoryAdjustmentException(
                            "Damage must have a negative quantity."
                    );
                }
            }
            case CORRECTION -> {
                // corrections can increase or decrease stock and this is  left here for doc purposes.
            }
        }
    }

    private InventoryMovementResponse toInventoryMovementResponse(
            InventoryMovement movement
    ) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getMovementType(),
                movement.getQuantityChange(),
                movement.getQuantityBefore(),
                movement.getQuantityAfter(),
                movement.getNote(),
                movement.getCreatedAt()
        );
    }

}