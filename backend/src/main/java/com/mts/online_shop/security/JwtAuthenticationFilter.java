package com.mts.online_shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveBearerToken(request)
                    .flatMap(jwtService::extractUserId)
                    .ifPresent(this::authenticateByUserId);
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(authHeader.substring(BEARER_PREFIX.length()).trim());
    }

    private void authenticateByUserId(Long userId) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                userId,
                null,
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
