package com.mts.online_shop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.camunda.bpm.engine.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CamundaSecurityBridgeFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CamundaSecurityBridgeFilter.class);

    private final IdentityService identityService;

    public CamundaSecurityBridgeFilter(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        String uri = request.getRequestURI();
        String rel = uri != null && uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
        return rel != null && (rel.startsWith("/camunda/app")
                || rel.startsWith("/camunda/api")
                || rel.startsWith("/app")
                || rel.startsWith("/engine-rest"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            try {
                String user = auth.getName();
                identityService.setAuthenticatedUserId(user);
                log.debug("Set Camunda authenticated user id from Spring Security: {}", user);
            } catch (Exception e) {
                log.warn("Failed to set Camunda authenticated user: {}", e.getMessage());
            }
        }

        try {
            try {
                filterChain.doFilter(request, response);
            } catch (IOException e) {
                String cls = e.getClass().getName();
                if (cls != null && cls.endsWith("ClientAbortException")) {
                    log.debug("Client aborted connection while writing response: {}", e.getMessage());
                } else {
                    throw e;
                }
            }
        } finally {
            try {
                identityService.clearAuthentication();
            } catch (Exception ignore) {
            }
        }
    }
}
