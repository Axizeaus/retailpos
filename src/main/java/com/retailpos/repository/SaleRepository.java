package com.retailpos.repository;

import com.retailpos.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByReceiptNumber(String receiptNumber);
}