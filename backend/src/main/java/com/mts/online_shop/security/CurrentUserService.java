package com.mts.online_shop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final XmlUserDetailsService xmlUserDetailsService;

    public CurrentUserService(XmlUserDetailsService xmlUserDetailsService) {
        this.xmlUserDetailsService = xmlUserDetailsService;
    }

    public Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        
        if (principal instanceof XmlUserPrincipal xmlUser) {
            return xmlUserDetailsService.findDatabaseUserIdByLogin(xmlUser.getUsername())
                    .or(() -> Optional.ofNullable(xmlUser.getUserId()));
        }
        
        if (principal instanceof JwtUserPrincipal jwtUser) {
            return xmlUserDetailsService.findDatabaseUserIdByLogin(jwtUser.username())
                    .or(() -> Optional.of(jwtUser.userId()));
        }

        // Handle Long directly (legacy)
        if (principal instanceof Long userId) {
            return Optional.of(userId);
        }
        
        // Handle String (fallback for compatibility)
        if (principal instanceof String value && !value.isBlank()) {
            try {
                return Optional.of(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Long getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> new com.mts.online_shop.exception.UnauthorizedException("Authentication required"));
    }

    public String getCurrentUserLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new com.mts.online_shop.exception.UnauthorizedException("Authentication required");
        }
        if (auth.getPrincipal() instanceof XmlUserPrincipal xmlUser) {
            return xmlUser.getUsername();
        }
        if (auth.getPrincipal() instanceof JwtUserPrincipal jwtUser) {
            return jwtUser.username();
        }
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User user) {
            return user.getUsername();
        }
        throw new com.mts.online_shop.exception.UnauthorizedException("Authentication required");
    }
}
