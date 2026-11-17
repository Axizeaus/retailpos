
package com.retailpos.controller;

import com.retailpos.dto.SaleResponse;
import com.retailpos.dto.SaleSummaryResponse;
import com.retailpos.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<Page<SaleSummaryResponse>> getSales(
            @RequestParam(required = false) String receiptNumber,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.clamp(size, 1, 100);

        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative.");
        }

        Pageable pageable = PageRequest.of(
                page,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        return ResponseEntity.ok(
                saleService.getSales(
                        receiptNumber,
                        startDate,
                        endDate,
                        pageable
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSaleById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @GetMapping("/receipt/{receiptNumber}")
    public ResponseEntity<SaleResponse> getSaleByReceiptNumber(
            @PathVariable String receiptNumber
    ) {
        return ResponseEntity.ok(
                saleService.getSaleByReceiptNumber(receiptNumber)
        );
    }
}
