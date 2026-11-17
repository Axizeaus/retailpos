package com.retailpos.exception;

public class SaleNotFoundException extends RuntimeException {

    public SaleNotFoundException(Long id) {
        super("Sale not found with ID: " + id);
    }

    public SaleNotFoundException(String receiptNumber) {
        super("Sale not found with receipt number: " + receiptNumber);
    }
}