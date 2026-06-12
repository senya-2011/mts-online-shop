package com.mts.online_shop.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Camunda REST (Jersey) на WildFly: Spring Boot не создаёт JerseyApplicationPath автоматически в WAR-режиме.
 */
@Configuration
public class CamundaJerseyConfiguration {

    @Bean
    @ConditionalOnMissingBean(JerseyApplicationPath.class)
    public JerseyApplicationPath jerseyApplicationPath() {
        return () -> "/engine-rest";
    }
}
