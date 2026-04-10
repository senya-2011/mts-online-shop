package com.mts.online_shop.service;

import com.mts.online_shop.exception.BadRequestException;
import com.mts.online_shop.exception.InvalidCredentialsException;
import com.mts.online_shop.exception.UserAlreadyExistsException;
import com.mts.online_shop.model.User;
import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.security.JwtService;
import com.mts.online_shop.security.XmlUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,64}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

    private final PasswordEncoder passwordEncoder;
    private final XmlUserDetailsService xmlUserDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserIdGeneratorService userIdGeneratorService;

    public AuthService(PasswordEncoder passwordEncoder, 
                      XmlUserDetailsService xmlUserDetailsService,
                      JwtService jwtService,
                      UserRepository userRepository,
                      UserIdGeneratorService userIdGeneratorService) {
        this.passwordEncoder = passwordEncoder;
        this.xmlUserDetailsService = xmlUserDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userIdGeneratorService = userIdGeneratorService;
    }

    public String authenticate(String login, String password) {
        UserDetails userDetails = xmlUserDetailsService.loadUserByUsername(login);
        
        // BCryptPasswordEncoder comparison
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        
        Long userId = xmlUserDetailsService.getUserIdByUsername(login);
        if (userId == null) {
            throw new InvalidCredentialsException("User not found");
        }
        
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
        
        return jwtService.generateToken(userId, login, roles, Collections.emptyMap());
    }

    @Transactional
    public Long register(String login, String email, String password, String name) {
        String normalizedLogin = normalizeLogin(login);
        String normalizedEmail = normalizeEmail(email);
        String rawPassword = normalizePassword(password);

        log.info("Starting registration for user: {}", normalizedLogin);

        if (xmlUserDetailsService.userExists(normalizedLogin)) {
            throw new UserAlreadyExistsException("User with login already exists");
        }

        // Hash password before saving
        String hashedPassword = passwordEncoder.encode(rawPassword);
        
        // Save to XML with hashed password (XmlUserDetailsService will generate ID)
        xmlUserDetailsService.saveUser(normalizedLogin, hashedPassword, Collections.singletonList("USER"));
        log.info("User saved to XML with hashed password: {}", normalizedLogin);
        
        // Get assigned ID from XML
        Long userId = xmlUserDetailsService.getUserIdByUsername(normalizedLogin);
        log.info("Retrieved userId from XML: {} for user: {}", userId, normalizedLogin);
        
        // Create user in database for business logic
        User dbUser = new User();
        dbUser.setId(userId); // Use same ID
        dbUser.setLogin(normalizedLogin);
        dbUser.setEmail(normalizedEmail);
        dbUser.setName(name);
        dbUser.setPasswordHash(hashedPassword); // Save hashed password
        dbUser.setRole("USER");
        
        User savedUser = userRepository.save(dbUser);
        log.info("User saved to database: {} with ID: {}", savedUser.getLogin(), savedUser.getId());
        
        log.info("Registration completed for user: {} with ID: {}", normalizedLogin, userId);
        
        return userId;
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
        if (normalized.length() > 255 || !EMAIL_PATTERN.matcher(normalized).matches()) {
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

    // ===== АДМИН-МЕТОДЫ для управления пользователями =====

    public List<User> getAllUsers() {
        log.debug("getAllUsers (admin)");
        return userRepository.findAll();
    }

    public User getUserById(Long userId) {
        log.debug("getUserById id={}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User with id: " + userId + " not found"));
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("deleteUser id={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User with id: " + userId + " not found"));
        userRepository.delete(user);
        log.info("User deleted id={}", userId);
    }
}
