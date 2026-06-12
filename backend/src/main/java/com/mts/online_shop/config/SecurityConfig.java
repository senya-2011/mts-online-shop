package com.mts.online_shop.config;

import com.mts.online_shop.security.CustomBasicAuthFilter;
import com.mts.online_shop.security.JwtAuthenticationFilter;
import com.mts.online_shop.security.XmlUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Camunda webapp keeps its own auth in {@link jakarta.servlet.http.HttpSession}.
     * Any Spring Security filter (even permitAll) can interfere with that session on WildFly.
     */
    @Bean
    public WebSecurityCustomizer camundaWebSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/camunda/**", "/engine-rest/**");
    }

    @Bean
    public AuthenticationManager authenticationManager(
            XmlUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
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
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint entryPoint,
            AccessDeniedHandler accessDeniedHandler,
            CustomBasicAuthFilter customBasicAuthFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            XmlUserDetailsService userDetailsService) throws Exception {
        http.securityMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/api/**"),
                        new AntPathRequestMatcher("/swagger-ui/**"),
                        new AntPathRequestMatcher("/v3/api-docs/**"),
                        new AntPathRequestMatcher("/")))
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
                .requestMatchers("/api/demo/telegram/**").permitAll()
                .requestMatchers("/api/internal/bank/**").permitAll()
                .requestMatchers("/", "/api", "/api/swagger-ui", "/api/swagger-ui/**", "/api/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/camunda/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/users/me/**").hasAnyRole("USER", "ADMIN")
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
