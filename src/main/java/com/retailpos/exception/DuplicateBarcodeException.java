package com.retailpos.exception;

public class DuplicateBarcodeException extends RuntimeException {

    public DuplicateBarcodeException(String barcode) {
        super("Barcode : "+barcode+" already exists. ");
    }
}