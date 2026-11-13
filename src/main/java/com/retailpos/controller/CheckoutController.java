package com.retailpos.controller;

import com.retailpos.dto.CheckoutRequest;
import com.retailpos.dto.SaleResponse;
import com.retailpos.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<SaleResponse> checkout(
            @Valid @RequestBody CheckoutRequest request
    ) {
        SaleResponse response = checkoutService.checkout(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}