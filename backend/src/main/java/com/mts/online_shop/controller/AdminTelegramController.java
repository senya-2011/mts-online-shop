package com.mts.online_shop.controller;

import com.mts.online_shop.model.TelegramLinkAdminItem;
import com.mts.online_shop.model.TelegramLinkBulkRequest;
import com.mts.online_shop.service.TelegramLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users/telegram")
@PreAuthorize("hasRole('ADMIN')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin-Telegram", description = "Массовая привязка Telegram")
public class AdminTelegramController {

    private final TelegramLinkService telegramLinkService;

    public AdminTelegramController(TelegramLinkService telegramLinkService) {
        this.telegramLinkService = telegramLinkService;
    }

    @GetMapping("/links")
    @io.swagger.v3.oas.annotations.Operation(summary = "Список привязок: telegramUsername — логин и имя пользователя")
    public ResponseEntity<List<TelegramLinkAdminItem>> listLinks() {
        return ResponseEntity.ok(telegramLinkService.listLinkedAccountsForAdmin());
    }

    @PostMapping("/bulk")
    @io.swagger.v3.oas.annotations.Operation(summary = "Привязать несколько пар userId / telegramUsername")
    public ResponseEntity<Void> bulkLink(@RequestBody TelegramLinkBulkRequest request) {
        if (request.getItems() == null) {
            return ResponseEntity.badRequest().build();
        }
        for (TelegramLinkBulkRequest.Entry entry : request.getItems()) {
            if (entry.getUserId() == null || entry.getTelegramUsername() == null) {
                continue;
            }
            telegramLinkService.linkTelegramForUser(entry.getUserId(), entry.getTelegramUsername());
        }
        return ResponseEntity.noContent().build();
    }
}
