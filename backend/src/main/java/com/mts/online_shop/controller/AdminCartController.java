package com.mts.online_shop.controller;

import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.security.annotation.RequirePrivilege;
import com.mts.online_shop.service.CartTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cart")
@SecurityRequirement(name = "basicAuth")
public class AdminCartController {

    private static final Logger log = LoggerFactory.getLogger(AdminCartController.class);
    private final CartTransactionService cartTransactionService;

    public AdminCartController(CartTransactionService cartTransactionService) {
        this.cartTransactionService = cartTransactionService;
    }

    @DeleteMapping("/user/{userId}")
    @RequirePrivilege("CART_MANAGE_ALL")
    public ResponseEntity<MessageResponse> clearUserCart(@PathVariable Long userId) {
        log.info("DELETE clear user cart {} (admin)", userId);
        cartTransactionService.clearCartCompletely(userId);
        MessageResponse response = new MessageResponse();
        response.setMessage("Корзина пользователя " + userId + " очищена");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/user/{userId}/items")
    @RequirePrivilege("CART_MANAGE_ALL")
    public ResponseEntity<MessageResponse> removeUserCartItems(@PathVariable Long userId, @RequestBody List<Long> itemIds) {
        log.info("DELETE remove items from user cart {} count={} (admin)", userId, itemIds.size());
        cartTransactionService.removeMultipleItems(userId, itemIds);
        MessageResponse response = new MessageResponse();
        response.setMessage("Удалено позиций из корзины пользователя " + userId + ": " + itemIds.size());
        return ResponseEntity.ok(response);
    }
}
