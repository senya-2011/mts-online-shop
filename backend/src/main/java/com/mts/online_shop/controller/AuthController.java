package com.mts.online_shop.controller;

import com.mts.online_shop.camunda.CamundaIdentityService;
import com.mts.online_shop.model.LoginRequest;
import com.mts.online_shop.model.LoginResponse;
import com.mts.online_shop.model.RegisterRequest;
import com.mts.online_shop.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "auth", description = "Аутентификация пользователей")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final CamundaIdentityService camundaIdentityService;

    public AuthController(AuthService authService, CamundaIdentityService camundaIdentityService) {
        this.authService = authService;
        this.camundaIdentityService = camundaIdentityService;
    }

    @PostMapping("/register")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Регистрация пользователя",
            description = "Создаёт учётную запись и возвращает JWT для доступа к API.")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest registerRequest) {
        log.info("POST register login={} email={}", registerRequest.getLogin(), registerRequest.getEmail());
        authService.register(
                registerRequest.getLogin(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getName());
        try {
            camundaIdentityService.syncRegisteredUser(
                    registerRequest.getLogin(),
                    registerRequest.getPassword(),
                    Collections.singleton("USER"));
        } catch (Exception e) {
            log.error("Camunda identity sync failed for user {}: {}", registerRequest.getLogin(), e.getMessage(), e);
        }
        String token = authService.issueTokenForUser(registerRequest.getLogin());
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.Operation(summary = "Аутентификация пользователя", description = "Выполняет вход пользователя и возвращает JWT токен для доступа к API")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("POST login login={}", loginRequest.getLogin());
        String token = authService.login(loginRequest.getLogin(), loginRequest.getPassword());
        try {
            camundaIdentityService.syncOnLogin(loginRequest.getLogin(), loginRequest.getPassword());
        } catch (Exception e) {
            log.warn("Camunda identity sync on login failed for {}: {}", loginRequest.getLogin(), e.getMessage());
        }
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        return ResponseEntity.ok(response);
    }
}
