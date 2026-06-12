package com.mts.online_shop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Редирект /camunda → Tasklist без регистрации Spring MVC handler (иначе ломается раздача webjars).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CamundaWebappRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        if ("/camunda".equals(path) || "/camunda/".equals(path)) {
            response.sendRedirect(contextPath + "/camunda/app/tasklist/default/");
            return;
        }
        chain.doFilter(request, response);
    }
}
