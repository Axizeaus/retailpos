package com.retailpos.exception;

public class ProductBarcodeNotFoundException extends RuntimeException {

    public ProductBarcodeNotFoundException(String barcode) {
        super("Product with barcode: " + barcode + " was not found.");
    }
}