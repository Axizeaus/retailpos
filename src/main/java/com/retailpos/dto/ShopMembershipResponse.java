package com.retailpos.dto;

import com.retailpos.enums.ShopRole;

public record ShopMembershipResponse(
        Long shopId,
        String shopName,
        String shopSlug,
        ShopRole role
) {
}