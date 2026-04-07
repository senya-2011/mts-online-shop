package com.mts.online_shop.controller;

import com.mts.online_shop.model.LoginRequest;
import com.mts.online_shop.model.LoginResponse;
import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.RegisterRequest;
import com.mts.online_shop.security.JwtService;
import com.mts.online_shop.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "auth", description = "Аутентификация пользователей")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JwtService jwtService;
    private final AuthService authService;

    public AuthController(JwtService jwtService, AuthService authService) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @PostMapping("/register")
    @io.swagger.v3.oas.annotations.Operation(summary = "Регистрация нового пользователя", description = "Создает новый аккаунт пользователя с логином, email и паролем")
    public ResponseEntity<MessageResponse> register(@RequestBody RegisterRequest registerRequest) {
        log.info("POST register login={} email={}", registerRequest.getLogin(), registerRequest.getEmail());
        try {
            Long userId = authService.register(
                    registerRequest.getLogin(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getName()
            );
            MessageResponse response = new MessageResponse();
            response.setMessage("Пользователь #" + userId + " зарегистрирован");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.warn("Registration failed for user {}: {}", registerRequest.getLogin(), e.getMessage());
            MessageResponse response = new MessageResponse();
            response.setMessage("Ошибка регистрации: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.Operation(summary = "Аутентификация пользователя", description = "Выполняет вход пользователя и возвращает JWT токен для доступа к API")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("POST login login={}", loginRequest.getLogin());
        try {
            String token = authService.authenticate(loginRequest.getLogin(), loginRequest.getPassword());
            LoginResponse response = new LoginResponse();
            response.setAccessToken(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Login failed for user {}: {}", loginRequest.getLogin(), e.getMessage());
            LoginResponse errorResponse = new LoginResponse();
            // Set accessToken to null to indicate error
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}
