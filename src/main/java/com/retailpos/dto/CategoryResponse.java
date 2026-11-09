package com.retailpos.dto;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        boolean active
) {
}