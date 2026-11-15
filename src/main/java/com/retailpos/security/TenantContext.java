package com.retailpos.security;

import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Setter
@Component
@RequestScope
public class TenantContext {

    private Long shopId;

    public Long getShopId() {
        if (shopId == null) {
            throw new IllegalStateException("No active shop in the current request");
        }
        return shopId;
    }

}