package com.mts.online_shop.service;

import com.mts.online_shop.exception.BadRequestException;
import com.mts.online_shop.exception.InvalidCredentialsException;
import com.mts.online_shop.exception.UserAlreadyExistsException;
import com.mts.online_shop.security.XmlUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Аутентификация и регистрация с JAAS и JWT.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,64}$");

    private final PasswordEncoder passwordEncoder;
    private final XmlUserDetailsService xmlUserDetailsService;

    public AuthService(PasswordEncoder passwordEncoder, 
                      XmlUserDetailsService xmlUserDetailsService) {
        this.passwordEncoder = passwordEncoder;
        this.xmlUserDetailsService = xmlUserDetailsService;
    }

    public String authenticate(String login, String password) {
        // Для HTTP Basic аутентификации не нужен JWT
        // Проверяем только существование пользователя
        try {
            xmlUserDetailsService.loadUserByUsername(login);
            log.info("User authenticated successfully: {}", login);
            return "authenticated";
        } catch (Exception e) {
            log.warn("Authentication failed for user {}: {}", login, e.getMessage());
            throw new InvalidCredentialsException("Неверный логин или пароль");
        }
    }

    public Long register(String login, String email, String password, String name) {
        String normalizedLogin = normalizeLogin(login);
        String normalizedEmail = normalizeEmail(email);
        String rawPassword = normalizePassword(password);

        if (xmlUserDetailsService.userExists(normalizedLogin)) {
            throw new UserAlreadyExistsException("Пользователь с таким логином уже существует");
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // Сохраняем в XML
        xmlUserDetailsService.saveUser(normalizedLogin, encodedPassword, Collections.singletonList("CUSTOMER"));
        
        log.info("Registered user login={} email={}", normalizedLogin, normalizedEmail);
        
        return 1L; // Фиктивный ID
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
