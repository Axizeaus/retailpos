package com.retailpos.repository;

    import com.retailpos.entity.Sale;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
    import java.util.Optional;

    public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

        Optional<Sale> findByIdAndShop_Id(Long id, Long shopId);

        Optional<Sale> findByReceiptNumberAndShop_Id(
                String receiptNumber,
                Long shopId
        );

    }
