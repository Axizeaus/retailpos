package com.retailpos.entity;

import com.retailpos.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="sales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    Long id;

    @Column(name="receipt_number", nullable = false, unique = true, length = 40)
    private String receiptNumber;

    @Column(name="total_amount", nullable = false, precision=12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name="cash_tendered", nullable = false, precision = 12, scale = 2)
    private BigDecimal cashTendered;

    @Column(name="change_due", nullable=false, precision=12, scale=2)
    private BigDecimal changeDue;

    @Enumerated(EnumType.STRING)
    @Column(name="payment_method", nullable=false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.PERSIST)
    private List<SaleItem> items = new ArrayList<>();

    public Sale(
            String receiptNumber,
            BigDecimal totalAmount,
            BigDecimal cashTendered,
            BigDecimal changeDue,
            PaymentMethod paymentMethod
    ) {
        this.receiptNumber = receiptNumber;
        this.totalAmount = totalAmount;
        this.cashTendered = cashTendered;
        this.changeDue = changeDue;
        this.paymentMethod = paymentMethod;
        this.createdAt = Instant.now();
    }

    public void addItem(SaleItem item) {
        items.add(item);
    }

}
