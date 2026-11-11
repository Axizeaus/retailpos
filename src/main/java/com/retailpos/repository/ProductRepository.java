package com.retailpos.repository;

import com.retailpos.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {

    boolean existsBySku(String sku);
    List<Product> findAllByActiveTrue();
    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    // lock the product while updating stock so concurrent requests don't overwrite each other's changes
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

}