package com.mts.online_shop.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.camunda.bpm.engine.rest.filter.CacheControlFilter;
import org.camunda.bpm.engine.rest.filter.EmptyBodyFilter;
import org.camunda.bpm.engine.rest.impl.CamundaRestResources;
import org.camunda.bpm.engine.rest.impl.FetchAndLockContextListener;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

/**
 * WAR на WildFly: Camunda REST в отдельном Jersey-сервлете без Spring Boot JerseyAutoConfiguration,
 * иначе REST и webapp (Cockpit/Tasklist) сливаются в один ResourceConfig.
 */
@Configuration
public class CamundaRestWarConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CamundaRestWarConfiguration.class);

    @Bean
    public FetchAndLockContextListener fetchAndLockContextListener() {
        return new FetchAndLockContextListener();
    }

    @Bean
    public ServletContextInitializer camundaRestFilterInitializer(JerseyApplicationPath applicationPath) {
        return servletContext -> registerRestFilters(servletContext, applicationPath.getUrlMapping());
    }

    @Bean
    public ServletRegistrationBean<ServletContainer> camundaEngineRestServlet() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, true);
        resourceConfig.registerClasses(CamundaRestResources.getResourceClasses());
        resourceConfig.registerClasses(CamundaRestResources.getConfigurationClasses());
        resourceConfig.register(JacksonFeature.class);

        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletRegistrationBean<ServletContainer> registration =
                new ServletRegistrationBean<>(servletContainer, "/engine-rest/*");
        registration.setName("CamundaEngineRestApi");
        registration.setLoadOnStartup(1);
        log.info("Registered isolated Camunda REST servlet at /engine-rest/*");
        return registration;
    }

    private static void registerRestFilters(ServletContext servletContext, String urlPattern) throws ServletException {
        registerFilter(servletContext, "CamundaRestEmptyBodyFilter", EmptyBodyFilter.class, urlPattern);
        registerFilter(servletContext, "CamundaRestCacheControlFilter", CacheControlFilter.class, urlPattern);
    }

    private static void registerFilter(
            ServletContext servletContext,
            String filterName,
            Class<? extends Filter> filterClass,
            String... urlPatterns) {
        FilterRegistration registration = servletContext.getFilterRegistration(filterName);
        if (registration == null) {
            registration = servletContext.addFilter(filterName, filterClass);
            registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns);
            log.debug("Filter {} mapped to {}", filterName, urlPatterns);
        }
    }

}
