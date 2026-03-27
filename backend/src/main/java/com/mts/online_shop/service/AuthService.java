package com.mts.online_shop.service;

import com.mts.online_shop.exception.BadRequestException;
import com.mts.online_shop.exception.InvalidCredentialsException;
import com.mts.online_shop.exception.UserAlreadyExistsException;
import com.mts.online_shop.security.JaasAuthenticationToken;
import com.mts.online_shop.security.JwtService;
import com.mts.online_shop.security.jaas.XmlUserLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Аутентификация и регистрация с JAAS и JWT.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,64}$");

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(PasswordEncoder passwordEncoder, 
                      AuthenticationManager authenticationManager,
                      JwtService jwtService) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public String authenticate(String login, String password) {
        String normalizedLogin = normalizeLogin(login);
        String rawPassword = normalizePassword(password);

        try {
            // Authenticate using JAAS
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedLogin, rawPassword)
            );

            // Extract user information
            String username = authentication.getName();
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // Extract additional user info from JAAS subject if available
            Map<String, Object> additionalClaims = new HashMap<>();
            if (authentication instanceof JaasAuthenticationToken) {
                var subject = ((JaasAuthenticationToken) authentication).getSubject();
                subject.getPrincipals().forEach(principal -> {
                    if (principal instanceof com.mts.online_shop.security.jaas.XmlUserPrincipal) {
                        var userPrincipal = (com.mts.online_shop.security.jaas.XmlUserPrincipal) principal;
                        additionalClaims.put("email", userPrincipal.getEmail());
                        additionalClaims.put("displayName", userPrincipal.getDisplayName());
                    }
                });
            }

            // Generate JWT token with roles and additional claims
            String token = jwtService.generateToken(null, username, roles, additionalClaims);
            
            log.info("User authenticated successfully: {}", normalizedLogin);
            return token;
            
        } catch (Exception e) {
            log.warn("Authentication failed for user {}: {}", normalizedLogin, e.getMessage());
            throw new InvalidCredentialsException("Неверный логин или пароль");
        }
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = {UserAlreadyExistsException.class, BadRequestException.class, RuntimeException.class})
    public Long register(String login, String email, String password, String name) {
        String normalizedLogin = normalizeLogin(login);
        String normalizedEmail = normalizeEmail(email);
        String rawPassword = normalizePassword(password);

        // Note: In a real JAAS + XML setup, registration would update the XML file
        // For this example, we'll throw an exception as XML file management is complex
        throw new BadRequestException("Регистрация через API временно отключена. Пожалуйста, обратитесь к администратору.");

        /*
        // Original registration logic (commented out for JAAS setup)
        if (userRepository.existsByLoginIgnoreCase(normalizedLogin)) {
            throw new UserAlreadyExistsException("Пользователь с таким логином уже существует");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
        }

        String normalizedName = name == null || name.isBlank() ? normalizedLogin : name.trim();
        if (normalizedName.length() > 255) {
            throw new BadRequestException("Имя пользователя слишком длинное");
        }

        User user = new User();
        user.setLogin(normalizedLogin);
        user.setEmail(normalizedEmail);
        user.setName(normalizedName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        User savedUser = userRepository.save(user);
        log.info("Registered user id={} login={} email={}", savedUser.getId(), normalizedLogin, normalizedEmail);
        
        return savedUser.getId();
        */
    }

    private String normalizeLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new BadRequestException("Логин обязателен");
        }
        String normalized = login.trim().toLowerCase(Locale.ROOT);
        if (!LOGIN_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("Логин должен быть 3-64 символа и содержать только буквы, цифры, '.', '_' или '-'");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email обязателен");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255 || !normalized.contains("@")) {
            throw new BadRequestException("Некорректный email");
        }
        return normalized;
    }

    private String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Пароль обязателен");
        }
        String normalized = password.trim();
        if (normalized.length() < 8 || normalized.length() > 72) {
            throw new BadRequestException("Пароль должен быть длиной от 8 до 72 символов");
        }
        return normalized;
    }
}
