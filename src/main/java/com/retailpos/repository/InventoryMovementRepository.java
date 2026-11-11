package com.retailpos.repository;

import com.retailpos.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findAllByProductIdOrderByCreatedAtDesc(
            Long productId,
            Pageable pageable
    );
}