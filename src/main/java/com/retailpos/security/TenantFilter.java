package com.retailpos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {


    private final TenantContext tenantContext;
    private final TenantAccessService tenantAccessService;

    @Override
    public void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String shopIdHeader = request.getHeader("X-Shop-Id");

            if (shopIdHeader == null || shopIdHeader.isBlank()) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "X-Shop-Id header is required."
                );
                return;
            }

            Long shopId;

            try {
                shopId = Long.parseLong(shopIdHeader);
            } catch (NumberFormatException exception) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "X-Shop-Id must be a valid number."
                );
                return;
            }

            Long userId;

            try {
                userId = Long.parseLong(jwtAuthentication.getToken().getSubject());
            } catch (NumberFormatException exception) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid token subject."
                );
                return;
            }

            boolean hasAccess = tenantAccessService.hasAccess(userId, shopId);

            if (!hasAccess) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "You do not have access to this shop."
                );
                return;
            }

            tenantContext.setShopId(shopId);
        }

        filterChain.doFilter(request, response);
    }
}