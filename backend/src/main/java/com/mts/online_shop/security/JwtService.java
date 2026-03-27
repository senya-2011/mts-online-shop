package com.mts.online_shop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final int MIN_HS256_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:default-secret-key-min-256-bits-for-hs256!!!!!!!!!}") String secret,
            @Value("${jwt.expiration-ms:31536000000}") long expirationMs) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_HS256_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String username, Set<String> roles, Map<String, Object> additionalClaims) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        
        long now = System.currentTimeMillis();
        
        Claims claims = Jwts.claims()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs));
        
        // Add additional claims
        if (additionalClaims != null) {
            additionalClaims.forEach(claims::claim);
        }
        
        return Jwts.builder()
                .claims(claims)
                .signWith(key)
                .compact();
    }
    
    public String generateToken(Long userId, String username, Set<String> roles) {
        return generateToken(userId, username, roles, null);
    }

    public Optional<Long> extractUserId(String token) {
        return extractClaims(token).map(claims -> {
            Object userId = claims.get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            return null;
        });
    }
    
    public Optional<String> extractUsername(String token) {
        return extractClaims(token).map(claims -> claims.get("username", String.class));
    }
    
    public Optional<Set<String>> extractRoles(String token) {
        return extractClaims(token).map(claims -> {
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            return roles != null ? new HashSet<>(roles) : Collections.emptySet();
        });
    }
    
    public Optional<String> extractEmail(String token) {
        return extractClaims(token).map(claims -> claims.get("email", String.class));
    }
    
    public Optional<String> extractDisplayName(String token) {
        return extractClaims(token).map(claims -> claims.get("displayName", String.class));
    }
    
    public boolean isTokenValid(String token) {
        return extractClaims(token).isPresent();
    }
    
    public boolean hasRole(String token, String role) {
        return extractRoles(token).map(roles -> roles.contains("ROLE_" + role)).orElse(false);
    }
    
    public boolean hasAnyRole(String token, String... roles) {
        Set<String> userRoles = extractRoles(token).orElse(Collections.emptySet());
        return Arrays.stream(roles)
                .map(role -> "ROLE_" + role)
                .anyMatch(userRoles::contains);
    }

    private Optional<Claims> extractClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
