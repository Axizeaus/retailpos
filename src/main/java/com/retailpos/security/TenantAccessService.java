package com.retailpos.security;

import com.retailpos.repository.ShopMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantAccessService {

    private final ShopMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public boolean hasAccess(Long userId, Long shopId) {
        return membershipRepository
                .findActiveMembershipWithActiveShop(userId, shopId)
                .isPresent();
    }
}