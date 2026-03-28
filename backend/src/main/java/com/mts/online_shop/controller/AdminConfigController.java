package com.mts.online_shop.controller;

import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.security.annotation.RequirePrivilege;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@SecurityRequirement(name = "basicAuth")
public class AdminConfigController {

    private static final Logger log = LoggerFactory.getLogger(AdminConfigController.class);

    @GetMapping
    @RequirePrivilege("SYSTEM_CONFIG")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        log.info("GET system config (admin)");
        Map<String, Object> config = new HashMap<>();
        config.put("app.name", "MTS Online Shop");
        config.put("app.version", "1.0.0");
        config.put("maintenance.mode", false);
        config.put("max.cart.items", 50);
        return ResponseEntity.ok(config);
    }

    @PutMapping
    @RequirePrivilege("SYSTEM_CONFIG")
    public ResponseEntity<MessageResponse> updateSystemConfig(@RequestBody Map<String, Object> config) {
        log.info("PUT update system config (admin): {}", config.keySet());
        // Логика обновления конфигурации
        MessageResponse response = new MessageResponse();
        response.setMessage("Конфигурация системы обновлена");
        return ResponseEntity.ok(response);
    }
}
