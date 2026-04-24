package com.mts.online_shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
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
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            log.debug("Processing request to: {}, Authorization header: {}", request.getRequestURI(), authHeader);
            
            resolveBearerToken(request).ifPresent(token -> {
                log.debug("Found JWT token: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
                jwtService.extractUserId(token).ifPresent(userId -> {
                    Set<String> roles = jwtService.extractRoles(token).orElse(Collections.emptySet());
                    log.debug("Extracted userId: {}, roles: {}", userId, roles);
                    authenticate(userId, roles);
                });
            });
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No valid Authorization header found");
            return Optional.empty();
        }
        return Optional.of(authHeader.substring(BEARER_PREFIX.length()).trim());
    }

    private void authenticate(Long userId, Set<String> roles) {
        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                userId,
                null,
                authorities
        );
        log.info("Authenticated user: {} with roles: {}", userId, roles);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
