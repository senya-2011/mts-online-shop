package com.mts.online_shop.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class CamundaWebappSecurityConfiguration {

    private static final String CSRF_PREVENTION_FILTER = "CsrfPreventionFilter";

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ServletContextInitializer camundaCsrfPreventionFilterOverride() {
        return servletContext -> servletContext.addFilter(
                CSRF_PREVENTION_FILTER,
                (request, response, chain) -> chain.doFilter(request, response));
    }
}
