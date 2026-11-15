
package com.retailpos.service;

import com.retailpos.dto.AuthResponse;
import com.retailpos.dto.LoginRequest;
import com.retailpos.dto.RegisterRequest;
import com.retailpos.dto.ShopMembershipResponse;
import com.retailpos.entity.Shop;
import com.retailpos.entity.ShopMembership;
import com.retailpos.entity.User;
import com.retailpos.enums.ShopRole;
import com.retailpos.repository.ShopMembershipRepository;
import com.retailpos.repository.ShopRepository;
import com.retailpos.repository.UserRepository;
import com.retailpos.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ShopMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists."
            );
        }

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes."
            );
        }

        User user = userRepository.save(
                new User(email, passwordEncoder.encode(request.password()))
        );

        Shop shop = shopRepository.save(
                new Shop(request.shopName().trim(), generateShopSlug(request.shopName()))
        );

        ShopMembership membership = membershipRepository.save(
                new ShopMembership(shop, user, ShopRole.OWNER)
        );

        return buildAuthResponse(user, List.of(membership));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(this::invalidCredentials);

        if (!user.isEnabled() || !passwordEncoder.matches(
                request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        List<ShopMembership> memberships = membershipRepository.findAllActiveMembershipsWithActiveShops(user.getId());

        return buildAuthResponse(user, memberships);
    }

    private AuthResponse buildAuthResponse(
            User user,
            List<ShopMembership> memberships
    ) {
        List<ShopMembershipResponse> membershipResponses = memberships.stream()
                .map(membership -> new ShopMembershipResponse(
                        membership.getShop().getId(),
                        membership.getShop().getName(),
                        membership.getShop().getSlug(),
                        membership.getRole()
                ))
                .toList();

        return new AuthResponse(
                jwtService.generateAccessToken(user.getId()),
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                user.getId(),
                user.getEmail(),
                membershipResponses
        );
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password."
        );
    }

    private String generateShopSlug(String shopName) {
        String base = shopName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (base.isBlank()) {
            base = "shop";
        }

        base = base.substring(0, Math.min(base.length(), 90))
                .replaceAll("-+$", "");

        return base + "-" + UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}
