package com.mts.online_shop.config;

import com.mts.online_shop.security.CustomBasicAuthFilter;
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
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
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
    public XmlUserDetailsService userDetailsService() {
        return new XmlUserDetailsService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(XmlUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Требуется авторизация\"}");
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Недостаточно прав\"}");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint entryPoint, AccessDeniedHandler accessDeniedHandler, CustomBasicAuthFilter customBasicAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> {
                ex.authenticationEntryPoint(entryPoint);
                ex.accessDeniedHandler(accessDeniedHandler);
            })
            .addFilterBefore(customBasicAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .userDetailsService(userDetailsService())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/products", "/products/**").permitAll()
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // СПЕЦИФИЧНЫЕ пути СНАЧАЛА (cart) - порядок ВАЖЕН!
                .requestMatchers("/cart/add/**").hasAuthority("PRIVILEGE_WRITE_CART")
                .requestMatchers("/cart/clear/**").hasAuthority("PRIVILEGE_CLEAR_CART")
                .requestMatchers("/cart/**").hasAuthority("PRIVILEGE_READ_CART")
                // СПЕЦИФИЧНЫЕ пути СНАЧАЛА (orders) - порядок ВАЖЕН!
                .requestMatchers("/orders/create").hasAuthority("PRIVILEGE_WRITE_ORDERS")
                .requestMatchers("/orders/process/**").hasAuthority("PRIVILEGE_PROCESS_ORDERS")
                .requestMatchers("/orders/cancel/**").hasAuthority("PRIVILEGE_CANCEL_ORDERS")
                .requestMatchers("/orders/**").hasAuthority("PRIVILEGE_READ_ORDERS")
                // Admin paths - специфичные сначала
                .requestMatchers("/users/create").hasAuthority("PRIVILEGE_WRITE_USERS")
                .requestMatchers("/users/delete/**").hasAuthority("PRIVILEGE_DELETE_USERS")
                .requestMatchers("/users/**").hasAuthority("PRIVILEGE_READ_USERS")
                .requestMatchers("/admin/products/delete/**").hasAuthority("PRIVILEGE_DELETE_PRODUCTS")
                .requestMatchers("/admin/products/**").hasAuthority("PRIVILEGE_WRITE_PRODUCTS")
                .requestMatchers("/admin/config/update").hasAuthority("PRIVILEGE_WRITE_SYSTEM_CONFIG")
                .requestMatchers("/admin/config/**").hasAuthority("PRIVILEGE_READ_SYSTEM_CONFIG")
                // Other paths
                .requestMatchers("/payments/**").hasAuthority("PRIVILEGE_PROCESS_PAYMENTS")
                .requestMatchers("/reports/**").hasAuthority("PRIVILEGE_VIEW_REPORTS")
                .requestMatchers("/transactions/**").hasAuthority("PRIVILEGE_PROCESS_ORDERS")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
