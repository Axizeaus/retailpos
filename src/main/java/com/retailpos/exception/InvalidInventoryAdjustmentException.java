package com.retailpos.exception;

public class InvalidInventoryAdjustmentException extends RuntimeException{
    public InvalidInventoryAdjustmentException(String message){
        super(message);
    }
}