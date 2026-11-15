
package com.retailpos.repository;

import com.retailpos.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByShopIdAndSku(Long shopId, String sku);

    boolean existsByShopIdAndSkuAndIdNot(
            Long shopId,
            String sku,
            Long id
    );

    Page<Product> findAllByShopIdAndActiveTrue(Long shopId, Pageable pageable);

    Optional<Product> findByIdAndShopId(Long id, Long shopId);

    boolean existsByShopIdAndCategoryIdAndActiveTrue(
            Long shopId,
            Long categoryId
    );

    boolean existsByShopIdAndBarcode(Long shopId, String barcode);

    boolean existsByShopIdAndBarcodeAndIdNot(
            Long shopId,
            String barcode,
            Long id
    );

    Optional<Product> findByShopIdAndBarcodeAndActiveTrue(
            Long shopId,
            String barcode
    );

    // Lock the product while updating stock so concurrent requests don't overwrite each other's changes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.id = :id
              AND p.shop.id = :shopId
            """)
    Optional<Product> findByIdForUpdate(
            @Param("id") Long id,
            @Param("shopId") Long shopId
    );
}
