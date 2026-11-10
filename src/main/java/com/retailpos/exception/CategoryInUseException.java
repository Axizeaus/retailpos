package com.retailpos.exception;

public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(Long id) {
        super("Category with id " + id + " cannot be deactivated because it has active products assigned to it.");
    }
}