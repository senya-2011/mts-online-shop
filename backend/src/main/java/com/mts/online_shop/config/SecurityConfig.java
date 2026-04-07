package com.mts.online_shop.config;

import com.mts.online_shop.security.CustomBasicAuthFilter;
import com.mts.online_shop.security.JwtAuthenticationFilter;
import com.mts.online_shop.security.PrivilegeService;
import com.mts.online_shop.security.XmlUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.jaas.JaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration;
import javax.security.auth.login.AppConfigurationEntry;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public XmlUserDetailsService userDetailsService(PrivilegeService privilegeService) {
        return new XmlUserDetailsService(privilegeService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        // Настройка JAAS Authentication Provider с программной конфигурацией
        JaasAuthenticationProvider jaasProvider = new JaasAuthenticationProvider();
        jaasProvider.setLoginContextName("MTSOnlineShop");
        
        // Создаем JAAS конфигурацию через Resource (jaas.conf файл)
        Resource jaasConfigResource = new ClassPathResource("jaas.conf");
        jaasProvider.setLoginConfig(jaasConfigResource);
        
        jaasProvider.setAuthorityGranters(new org.springframework.security.authentication.jaas.AuthorityGranter[] {
            principal -> {
                // Извлекаем роли из JAAS Principal
                if (principal instanceof com.mts.online_shop.security.jaas.XmlUserPrincipal) {
                    com.mts.online_shop.security.jaas.XmlUserPrincipal xmlPrincipal = 
                        (com.mts.online_shop.security.jaas.XmlUserPrincipal) principal;
                    return xmlPrincipal.getUser().getRoles();
                }
                return java.util.Collections.emptySet();
            }
        });
        try {
            jaasProvider.afterPropertiesSet();
        } catch (Exception e) {
            log.error("Failed to initialize JAAS provider: {}", e.getMessage(), e);
            throw new RuntimeException("JAAS initialization failed", e);
        }
        return new ProviderManager(jaasProvider);
    }

    private javax.security.auth.login.Configuration createJaasConfiguration() {
        Map<String, String> options = new java.util.HashMap<>();
        options.put("usersFile", "classpath:users.xml");
        options.put("debug", "true");
        
        javax.security.auth.login.AppConfigurationEntry entry = new javax.security.auth.login.AppConfigurationEntry(
            "com.mts.online_shop.security.jaas.XmlUserLoginModule",
            javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
            options
        );
        
        return new org.springframework.security.authentication.jaas.memory.InMemoryConfiguration(
            java.util.Map.of("MTSOnlineShop", new javax.security.auth.login.AppConfigurationEntry[] { entry })
        );
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Требуется авторизация\"}");
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Недостаточно прав\"}");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint entryPoint, AccessDeniedHandler accessDeniedHandler, CustomBasicAuthFilter customBasicAuthFilter, JwtAuthenticationFilter jwtAuthenticationFilter, XmlUserDetailsService userDetailsService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> {
                ex.authenticationEntryPoint(entryPoint);
                ex.accessDeniedHandler(accessDeniedHandler);
            })
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(customBasicAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/products", "/api/products/**").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/swagger-ui/**", "/api/v3/api-docs/**", "/api/swagger-ui.html", "/api/swagger-ui/**").permitAll()
                // Cart - USER and ADMIN
                .requestMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")
                // Orders - USER and ADMIN
                .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
                // Admin only
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/payments/**").hasRole("ADMIN")
                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                .requestMatchers("/api/transactions/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
