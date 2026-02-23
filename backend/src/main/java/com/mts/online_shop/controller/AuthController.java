package com.mts.online_shop.controller;

import com.mts.online_shop.api.AuthApi;
import com.mts.online_shop.model.LoginRequest;
import com.mts.online_shop.model.LoginResponse;
import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.RegisterRequest;
import com.mts.online_shop.security.JwtService;
import com.mts.online_shop.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JwtService jwtService;
    private final AuthService authService;

    public AuthController(JwtService jwtService, AuthService authService) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        log.info("POST login login={}", loginRequest.getLogin());
        Long userId = authService.authenticate(loginRequest.getLogin(), loginRequest.getPassword());
        String token = jwtService.generateToken(userId);
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MessageResponse> register(RegisterRequest registerRequest) {
        log.info("POST register login={} email={}", registerRequest.getLogin(), registerRequest.getEmail());
        authService.register(
                registerRequest.getLogin(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getName()
        );
        MessageResponse response = new MessageResponse();
        response.setMessage("Пользователь зарегистрирован");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
