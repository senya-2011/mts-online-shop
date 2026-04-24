package com.mts.online_shop.controller;

import com.mts.online_shop.model.TelegramUsernameRequest;
import com.mts.online_shop.security.CurrentUserService;
import com.mts.online_shop.service.TelegramLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@io.swagger.v3.oas.annotations.tags.Tag(name = "User-Telegram", description = "Привязка Telegram к текущему пользователю")
public class UserMeTelegramController {

    private final CurrentUserService currentUserService;
    private final TelegramLinkService telegramLinkService;

    public UserMeTelegramController(CurrentUserService currentUserService, TelegramLinkService telegramLinkService) {
        this.currentUserService = currentUserService;
        this.telegramLinkService = telegramLinkService;
    }

    @PutMapping("/telegram")
    @io.swagger.v3.oas.annotations.Operation(summary = "Привязать Telegram username")
    public ResponseEntity<Void> linkTelegram(@RequestBody TelegramUsernameRequest request) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        telegramLinkService.linkTelegramForUser(userId, request.getTelegramUsername());
        return ResponseEntity.noContent().build();
    }
}
