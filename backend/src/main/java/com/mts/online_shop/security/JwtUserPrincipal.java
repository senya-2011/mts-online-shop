package com.mts.online_shop.security;

/**
 * Principal for requests authenticated via JWT Bearer token.
 */
public record JwtUserPrincipal(Long userId, String username) {
}
