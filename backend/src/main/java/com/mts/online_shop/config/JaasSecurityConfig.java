package com.mts.online_shop.config;

import com.mts.online_shop.security.JaasAuthenticationProvider;
import com.mts.online_shop.security.JwtAuthenticationFilter;
import com.mts.online_shop.security.PrivilegeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Set;

@Configuration
@EnableWebSecurity
public class JaasSecurityConfig {

    private final PrivilegeService privilegeService;

    public JaasSecurityConfig(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JaasAuthenticationProvider jaasAuthenticationProvider() {
        JaasAuthenticationProvider provider = new JaasAuthenticationProvider();
        provider.setConfigFile("classpath:jaas.conf");
        provider.setContextName("MTSOnlineShop");
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Set.of(jaasAuthenticationProvider()));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(authenticationManager())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/problem+json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                            "{\"type\":\"https://api.mts-online-shop.example/errors/unauthorized\"," +
                                    "\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Authentication required\"}"
                    );
                }))
                .authorizeHttpRequests(auth -> auth
                        // Открытые эндпоинты
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/products", "/api/products/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        
                        // Требуют аутентификации
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders").authenticated()
                        .requestMatchers("/api/orders/{id}").authenticated()
                        .requestMatchers("/api/cart-transaction/clear").authenticated()
                        .requestMatchers("/api/cart-transaction/items").authenticated()
                        
                        // Требуют ролей
                        .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/orders/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/admin/config/**").hasRole("ADMIN")
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/payments/process").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/payments/refund/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/cart-transaction/merge/**").hasRole("ADMIN")
                        
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
