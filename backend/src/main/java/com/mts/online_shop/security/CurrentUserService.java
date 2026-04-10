package com.mts.online_shop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    public Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        
        // Handle XmlUserPrincipal
        if (principal instanceof XmlUserPrincipal xmlUser) {
            return Optional.of(xmlUser.getUserId());
        }
        
        // Handle Long directly
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
}
