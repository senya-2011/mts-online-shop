package com.mts.online_shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

@Component
public class CustomBasicAuthFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public CustomBasicAuthFilter(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");
        
        if (header != null && header.startsWith("Basic ")) {
            try {
                String base64Credentials = header.substring("Basic ".length());
                String credentials = new String(Base64.getDecoder().decode(base64Credentials));
                String[] values = credentials.split(":", 2);
                
                if (values.length == 2) {
                    String username = values[0];
                    String password = values[1];
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (passwordEncoder.matches(password, userDetails.getPassword()) || 
                        password.equals(userDetails.getPassword())) {
                        UsernamePasswordAuthenticationToken auth = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                // Не авторизован - продолжаем без аутентификации
            }
        }
        
        chain.doFilter(request, response);
    }
}
