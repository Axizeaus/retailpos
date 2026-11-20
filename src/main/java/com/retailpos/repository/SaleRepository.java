package com.retailpos.repository;

    import com.retailpos.entity.Sale;
    import com.retailpos.repository.projection.PaymentMethodSalesProjection;
    import com.retailpos.repository.projection.SalesSummaryProjection;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import java.time.Instant;
    import java.util.List;
    import java.util.Optional;

    public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

        Optional<Sale> findByIdAndShop_Id(Long id, Long shopId);

        Optional<Sale> findByReceiptNumberAndShop_Id(
                String receiptNumber,
                Long shopId
        );


        @Query("""
        SELECT
            COUNT(s.id) AS transactionCount,
            SUM(s.totalAmount) AS totalSales,
            SUM(s.cashTendered) AS totalCashTendered,
            SUM(s.changeDue) AS totalChangeGiven
        FROM Sale s
        WHERE s.shop.id = :shopId
          AND s.createdAt >= :start
          AND s.createdAt < :end
        """)
        SalesSummaryProjection getDailySalesSummary(
                @Param("shopId") Long shopId,
                @Param("start") Instant start,
                @Param("end") Instant end
        );

        @Query("""
        SELECT
            s.paymentMethod AS paymentMethod,
            COUNT(s.id) AS transactionCount,
            SUM(s.totalAmount) AS totalSales
        FROM Sale s
        WHERE s.shop.id = :shopId
          AND s.createdAt >= :start
          AND s.createdAt < :end
        GROUP BY s.paymentMethod
        ORDER BY s.paymentMethod
        """)
        List<PaymentMethodSalesProjection> getDailyPaymentMethodBreakdown(
                @Param("shopId") Long shopId,
                @Param("start") Instant start,
                @Param("end") Instant end
        );


    }
