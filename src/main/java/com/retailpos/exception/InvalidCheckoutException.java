package com.retailpos.exception;

public class InvalidCheckoutException extends RuntimeException {

    public InvalidCheckoutException(String message) {
        super(message);
    }
}