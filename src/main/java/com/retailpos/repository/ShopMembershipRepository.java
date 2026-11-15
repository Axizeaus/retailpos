
package com.retailpos.repository;

import com.retailpos.entity.ShopMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopMembershipRepository extends JpaRepository<ShopMembership, Long> {

    @Query("""
        SELECT m
        FROM ShopMembership m
        JOIN FETCH m.shop s
        WHERE m.user.id = :userId
          AND s.id = :shopId
          AND m.active = true
          AND s.active = true
        """)
    Optional<ShopMembership> findActiveMembershipWithActiveShop(
            @Param("userId") Long userId,
            @Param("shopId") Long shopId
    );

    @Query("""
        SELECT m
        FROM ShopMembership m
        JOIN FETCH m.shop s
        WHERE m.user.id = :userId
          AND m.active = true
          AND s.active = true
        """)
    List<ShopMembership> findAllActiveMembershipsWithActiveShops(
            @Param("userId") Long userId
    );
}
