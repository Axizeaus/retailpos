package com.retailpos.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String name,
        String email,
        String phoneNumber,
        List<ShopMembershipResponse> memberships
) {
}