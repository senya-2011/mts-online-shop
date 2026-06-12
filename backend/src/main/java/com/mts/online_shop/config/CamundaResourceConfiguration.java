package com.mts.online_shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Статика Camunda webapp (WildFly). Не включать {@code /camunda/api/**} — там Jersey auth API.
 */
@Configuration
public class CamundaResourceConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = "classpath:/META-INF/resources/webjars/camunda/";
        registry.addResourceHandler(
                        "/camunda/app/**",
                        "/camunda/lib/**",
                        "/camunda/assets/**",
                        "/camunda/index.html",
                        "/camunda/favicon.ico")
                .addResourceLocations(base);
    }
}
