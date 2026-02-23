package com.mts.online_shop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

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

    public String generateToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    public Optional<Long> extractUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();

            Object userId = claims.get("userId");
            if (userId instanceof Number) {
                return Optional.of(((Number) userId).longValue());
            }

            String sub = claims.getSubject();
            if (sub != null && !sub.isBlank()) {
                return Optional.of(Long.parseLong(sub));
            }
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
