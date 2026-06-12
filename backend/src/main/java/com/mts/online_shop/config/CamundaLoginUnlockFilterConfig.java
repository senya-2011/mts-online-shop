package com.mts.online_shop.config;

import com.mts.online_shop.camunda.CamundaIdentityService;
import com.mts.online_shop.camunda.CamundaLoginUnlockFilter;
import com.mts.online_shop.camunda.CamundaPreLoginSyncFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import com.mts.online_shop.camunda.CamundaIdentityPasswordWriter;

@Configuration
public class CamundaLoginUnlockFilterConfig {

    private static final String CAMUNDA_LOGIN_URL = "/camunda/api/admin/auth/*";

    @Bean
    public FilterRegistrationBean<CamundaLoginUnlockFilter> camundaLoginUnlockFilter(
            CamundaIdentityPasswordWriter passwordWriter) {
        FilterRegistrationBean<CamundaLoginUnlockFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CamundaLoginUnlockFilter(passwordWriter));
        registration.addUrlPatterns(CAMUNDA_LOGIN_URL);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("camundaLoginUnlockFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CamundaPreLoginSyncFilter> camundaPreLoginSyncFilter(
            CamundaIdentityService camundaIdentityService) {
        FilterRegistrationBean<CamundaPreLoginSyncFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CamundaPreLoginSyncFilter(camundaIdentityService));
        registration.addUrlPatterns(CAMUNDA_LOGIN_URL);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("camundaPreLoginSyncFilter");
        return registration;
    }
}
