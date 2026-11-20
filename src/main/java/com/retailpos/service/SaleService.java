package com.retailpos.service;

import com.retailpos.dto.*;
import com.retailpos.entity.Sale;
import com.retailpos.entity.Shop;
import com.retailpos.exception.SaleNotFoundException;
import com.retailpos.repository.SaleRepository;
import com.retailpos.repository.ShopRepository;
import com.retailpos.repository.projection.PaymentMethodSalesProjection;
import com.retailpos.repository.projection.SalesSummaryProjection;
import com.retailpos.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleService {
    private final SaleRepository saleRepository;
    private final ShopRepository shopRepository;
    private final TenantContext tenantContext;

    public DailySalesSummaryResponse getDailySalesSummary(LocalDate businessDate) {
        Long shopId = tenantContext.getShopId();

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated shop could not be found."
                ));

        ZoneId zone = ZoneId.of(shop.getTimeZone());

        LocalDate reportDate = businessDate != null
                ? businessDate
                : LocalDate.now(zone);

        Instant start = reportDate.atStartOfDay(zone).toInstant();
        Instant end = reportDate.plusDays(1)
                .atStartOfDay(zone)
                .toInstant();

        SalesSummaryProjection summary = saleRepository.getDailySalesSummary(
                shopId,
                start,
                end
        );

        Long transactionCount = summary.getTransactionCount();

        BigDecimal totalSales = zeroIfNull(summary.getTotalSales());
        BigDecimal totalCashTendered =
                zeroIfNull(summary.getTotalCashTendered());
        BigDecimal totalChangeGiven =
                zeroIfNull(summary.getTotalChangeGiven());

        BigDecimal averageTransactionValue = transactionCount == 0
                ? BigDecimal.ZERO
                : totalSales.divide(
                BigDecimal.valueOf(transactionCount),
                2,
                RoundingMode.HALF_UP
        );

        List<PaymentMethodSalesResponse> paymentMethods =
                saleRepository.getDailyPaymentMethodBreakdown(
                                shopId,
                                start,
                                end
                        )
                        .stream()
                        .map(this::toPaymentMethodSalesResponse)
                        .toList();

        return new DailySalesSummaryResponse(
                reportDate,
                transactionCount,
                totalSales,
                averageTransactionValue,
                totalCashTendered,
                totalChangeGiven,
                paymentMethods
        );
    }

    private PaymentMethodSalesResponse toPaymentMethodSalesResponse(
            PaymentMethodSalesProjection projection
    ) {
        return new PaymentMethodSalesResponse(
                projection.getPaymentMethod(),
                projection.getTransactionCount(),
                zeroIfNull(projection.getTotalSales())
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }


    public Page<SaleSummaryResponse> getSales(
            String receiptNumber,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Long shopId = tenantContext.getShopId();

        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must not be after end date."
            );
        }

        String normalizedReceiptNumber =
                receiptNumber == null || receiptNumber.isBlank()
                        ? null
                        : receiptNumber.trim();

        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null || endDate != null) {
            Shop shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Authenticated shop could not be found."
                    ));

            ZoneId zone = ZoneId.of(shop.getTimeZone());

            if (startDate != null) {
                startInstant = startDate.atStartOfDay(zone).toInstant();
            }

            if (endDate != null) {
                endInstant = endDate.plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant();
            }
        }

        Instant finalStartInstant = startInstant;
        Instant finalEndInstant = endInstant;

        Specification<Sale> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    criteriaBuilder.equal(root.<Long>get("shop").get("id"), shopId)
            );

            if (normalizedReceiptNumber != null) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("receiptNumber")),
                                "%" + normalizedReceiptNumber.toLowerCase(Locale.ROOT) + "%"
                        )
                );
            }

            if (finalStartInstant != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                finalStartInstant
                        )
                );
            }

            if (finalEndInstant != null) {
                predicates.add(
                        criteriaBuilder.lessThan(
                                root.get("createdAt"),
                                finalEndInstant
                        )
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Sale> sales = saleRepository.findAll(specification, pageable);

        return sales.map(this::toSaleSummaryResponse);
    }


    public SaleResponse getSaleById(Long id) {
        Long shopId = tenantContext.getShopId();

        Sale sale = saleRepository.findByIdAndShop_Id(id, shopId)
                .orElseThrow(() -> new SaleNotFoundException(id));

        return toSaleResponse(sale);
    }

    public SaleResponse getSaleByReceiptNumber(String receiptNumber) {
        Long shopId = tenantContext.getShopId();

        String normalizedReceiptNumber =
                receiptNumber == null ? "" : receiptNumber.trim();

        if (normalizedReceiptNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Receipt number must not be blank."
            );
        }

        Sale sale = saleRepository.findByReceiptNumberAndShop_Id(
                        normalizedReceiptNumber,
                        shopId
                )
                .orElseThrow(() ->
                        new SaleNotFoundException(normalizedReceiptNumber)
                );

        return toSaleResponse(sale);
    }

    private SaleSummaryResponse toSaleSummaryResponse(Sale sale){
        return new SaleSummaryResponse(
                sale.getId(),
                sale.getReceiptNumber(),
                sale.getTotalAmount(),
                sale.getPaymentMethod(),
                sale.getCreatedAt()
        );
    }

    private SaleResponse toSaleResponse(Sale sale) {
        List<SaleItemResponse> items = sale.getItems()
                .stream()
                .map(item -> new SaleItemResponse(
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getProductSku(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();

        return new SaleResponse(
                sale.getId(),
                sale.getReceiptNumber(),
                sale.getTotalAmount(),
                sale.getCashTendered(),
                sale.getChangeDue(),
                sale.getPaymentMethod(),
                sale.getCreatedAt(),
                items
        );
    }
}