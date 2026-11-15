package com.retailpos.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String email,
        List<ShopMembershipResponse> memberships
) {
}