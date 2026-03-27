package com.mts.online_shop.service;

import com.mts.online_shop.exception.BadRequestException;
import com.mts.online_shop.exception.InvalidCredentialsException;
import com.mts.online_shop.exception.UserAlreadyExistsException;
import com.mts.online_shop.model.User;
import com.mts.online_shop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.transaction.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Аутентификация и регистрация.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,64}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Long authenticate(String login, String password) {
        String normalizedLogin = normalizeLogin(login);
        String rawPassword = normalizePassword(password);
        User user = userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase(normalizedLogin, normalizedLogin)
                .orElseThrow(() -> new InvalidCredentialsException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Неверный логин или пароль");
        }

        return user.getId();
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = {UserAlreadyExistsException.class, BadRequestException.class, RuntimeException.class})
    public Long register(String login, String email, String password, String name) {
        String normalizedLogin = normalizeLogin(login);
        String normalizedEmail = normalizeEmail(email);
        String rawPassword = normalizePassword(password);

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
