package com.mts.online_shop.camunda;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;

/**
 * Сбрасывает lock/attempts в act_id_user перед Camunda login POST (отдельная транзакция).
 */
public class CamundaLoginUnlockFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CamundaLoginUnlockFilter.class);

    private final CamundaIdentityPasswordWriter passwordWriter;

    public CamundaLoginUnlockFilter(CamundaIdentityPasswordWriter passwordWriter) {
        this.passwordWriter = passwordWriter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && isCamundaCredentialLoginPost(httpRequest)) {
            int unlocked = passwordWriter.unlockAllLockedUsers();
            if (unlocked > 0) {
                log.debug("Camunda pre-login unlock: cleared lock for {} user(s)", unlocked);
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean isCamundaCredentialLoginPost(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null || !uri.contains("/camunda/api/admin/auth/user/")) {
            return false;
        }
        int loginIdx = uri.lastIndexOf("/login");
        if (loginIdx < 0) {
            return false;
        }
        return uri.substring(loginIdx + "/login".length()).isEmpty();
    }
}
