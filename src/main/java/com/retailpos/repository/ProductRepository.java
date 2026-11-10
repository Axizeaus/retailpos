package com.retailpos.repository;

import com.retailpos.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product,Long> {

    boolean existsBySku(String sku);
    List<Product> findAllByActiveTrue();
    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

}