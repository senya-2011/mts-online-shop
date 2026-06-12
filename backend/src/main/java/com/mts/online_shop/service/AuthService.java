package com.mts.online_shop.service;

import com.mts.online_shop.camunda.CamundaIdentityService;
import com.mts.online_shop.exception.BadRequestException;
import com.mts.online_shop.exception.InvalidCredentialsException;
import com.mts.online_shop.exception.UserAlreadyExistsException;
import com.mts.online_shop.model.User;
import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.security.JwtService;
import com.mts.online_shop.security.XmlUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
    private final UserRepository userRepository;
    private final UserIdGeneratorService userIdGeneratorService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CamundaIdentityService camundaIdentityService;

    public AuthService(PasswordEncoder passwordEncoder,
                      XmlUserDetailsService xmlUserDetailsService,
                      UserRepository userRepository,
                      UserIdGeneratorService userIdGeneratorService,
                      AuthenticationManager authenticationManager,
                      JwtService jwtService,
                      CamundaIdentityService camundaIdentityService) {
        this.passwordEncoder = passwordEncoder;
        this.xmlUserDetailsService = xmlUserDetailsService;
        this.userRepository = userRepository;
        this.userIdGeneratorService = userIdGeneratorService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.camundaIdentityService = camundaIdentityService;
    }

    public String login(String login, String password) {
        String normalizedLogin = normalizeLogin(login);
        String rawPassword = normalizePassword(password);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedLogin, rawPassword));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return issueTokenForUser(normalizedLogin);
    }

    public String issueTokenForUser(String login) {
        return generateTokenForLogin(normalizeLogin(login));
    }

    private String generateTokenForLogin(String normalizedLogin) {
        Long userId = xmlUserDetailsService.ensureDatabaseUserIdByLogin(normalizedLogin).orElse(null);
        if (userId == null) {
            throw new InvalidCredentialsException("User not found in shop database");
        }
        UserDetails userDetails = xmlUserDetailsService.loadUserByUsername(normalizedLogin);
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
        return jwtService.generateToken(userId, normalizedLogin, roles, Collections.emptyMap());
    }

    @Transactional
    public Long register(String login, String email, String password, String name) {
        String normalizedLogin = normalizeLogin(login);
        String normalizedEmail = normalizeEmail(email);
        String rawPassword = normalizePassword(password);

        log.info("Starting registration for user: {}", normalizedLogin);

        if (userRepository.existsByLoginIgnoreCase(normalizedLogin)) {
            throw new UserAlreadyExistsException("User with login already exists");
        }
        if (xmlUserDetailsService.userExists(normalizedLogin)) {
            log.warn("Removing orphan XML user '{}' (missing in DB after previous failed registration)", normalizedLogin);
            xmlUserDetailsService.removeUser(normalizedLogin);
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email already exists");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        Long maxDbId = userRepository.findMaxUserId();
        long minId = maxDbId != null ? maxDbId : 0L;
        xmlUserDetailsService.saveUserWithId(
                normalizedLogin, hashedPassword, Collections.singletonList("USER"),
                xmlUserDetailsService.allocateNextUserId(minId));
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

    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User with id: " + userId + " not found"));
        user.setRole("BANNED");
        userRepository.save(user);
    }

    @Transactional
    public void changeUserRole(Long userId, String targetRole) {
        if (targetRole == null || targetRole.isBlank()) {
            throw new BadRequestException("targetRole is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User with id: " + userId + " not found"));
        String normalizedRole = targetRole.trim().toUpperCase(Locale.ROOT);
        user.setRole(normalizedRole);
        userRepository.save(user);
        xmlUserDetailsService.syncRoleForLogin(user.getLogin(), normalizedRole);
        camundaIdentityService.resyncShopUser(user.getLogin());
    }
}
