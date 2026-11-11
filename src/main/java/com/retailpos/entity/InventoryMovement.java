package com.retailpos.entity;

import com.retailpos.enums.InventoryMovementType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryMovement{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name="movement_type", nullable = false, length=30)
    private InventoryMovementType movementType;

    @Column(name="quantity_change", nullable = false)
    private int quantityChange;

    @Column(name="quantity_before", nullable = false)
    private int quantityBefore;

    @Column(name="quantity_after", nullable = false)
    private int quantityAfter;

    @Column(columnDefinition = "TEXT" )
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InventoryMovement(
            Product product,
            InventoryMovementType movementType,
            int quantityChange,
            int quantityBefore,
            int quantityAfter,
            String note
    ){
        this.product = product;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.note = note;
        this.createdAt = Instant.now();
    }
}
