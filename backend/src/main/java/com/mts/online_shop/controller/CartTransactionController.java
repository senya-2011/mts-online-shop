package com.mts.online_shop.controller;

import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.service.CartTransactionService;
import com.mts.online_shop.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart-transaction")
@SecurityRequirement(name = "basicAuth")
public class CartTransactionController {

    private static final Logger log = LoggerFactory.getLogger(CartTransactionController.class);
    private final CartTransactionService cartTransactionService;
    private final CurrentUserService currentUserService;

    public CartTransactionController(CartTransactionService cartTransactionService, 
                                   CurrentUserService currentUserService) {
        this.cartTransactionService = cartTransactionService;
        this.currentUserService = currentUserService;
    }

    @DeleteMapping("/clear")
    public ResponseEntity<MessageResponse> clearCartCompletely() {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.info("DELETE clear cart completely userId={}", userId);
        cartTransactionService.clearCartCompletely(userId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Корзина полностью очищена");
        return ResponseEntity.ok(msg);
    }

    @DeleteMapping("/items")
    public ResponseEntity<MessageResponse> removeMultipleItems(@RequestBody List<Long> itemIds) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.info("DELETE multiple cart items userId={} count={}", userId, itemIds.size());
        cartTransactionService.removeMultipleItems(userId, itemIds);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Удалено позиций: " + itemIds.size());
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/merge/{sourceUserId}")
    public ResponseEntity<MessageResponse> mergeCarts(@PathVariable Long sourceUserId) {
        Long targetUserId = currentUserService.getCurrentUserIdOrThrow();
        log.info("POST merge carts targetUserId={} sourceUserId={}", targetUserId, sourceUserId);
        cartTransactionService.mergeCarts(targetUserId, sourceUserId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Корзины успешно объединены");
        return ResponseEntity.ok(msg);
    }
}
