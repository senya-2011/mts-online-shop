package com.mts.online_shop.controller;

import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.UserListResponse;
import com.mts.online_shop.model.User;
import com.mts.online_shop.model.UserItem;
import com.mts.online_shop.model.CartWithUserResponse;
import com.mts.online_shop.service.AuthService;
import com.mts.online_shop.service.GoodsService;
import com.mts.online_shop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin-Users", description = "Управление пользователями")
public class AdminUsersController {

    private static final Logger log = LoggerFactory.getLogger(AdminUsersController.class);
    private final AuthService authService;
    private final GoodsService goodsService;
    private final OrderService orderService;

    public AdminUsersController(AuthService authService, GoodsService goodsService, OrderService orderService) {
        this.authService = authService;
        this.goodsService = goodsService;
        this.orderService = orderService;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить пользователей", description = "Возвращает список всех пользователей системы")
    public ResponseEntity<UserListResponse> getUsers(@RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        log.debug("GET admin users page={} size={}", page, size);
        List<User> users = authService.getAllUsers();
        UserListResponse response = new UserListResponse();
        response.setItems(users);
        response.setTotal((long) users.size());
        response.setPage(page != null ? page : 0);
        response.setSize(size != null ? size : 20);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить пользователя", description = "Возвращает детальную информацию о конкретном пользователе")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        log.debug("GET admin user id={}", userId);
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/cart")
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить корзины", description = "Возвращает информацию о корзинах пользователей")
    public ResponseEntity<List<CartWithUserResponse>> getCart() {
        log.debug("GET admin cart");
        // Получаем все корзины всех пользователей с группировкой
        List<CartWithUserResponse> carts = goodsService.getAllCartsWithUsers();
        return ResponseEntity.ok(carts);
    }

    @DeleteMapping("/{userId}/cart/clear")
    @io.swagger.v3.oas.annotations.Operation(summary = "Очистить корзину пользователя", description = "Полностью очищает корзину указанного пользователя")
    public ResponseEntity<MessageResponse> clearUserCart(@PathVariable Long userId) {
        log.debug("DELETE admin user {} cart", userId);
        goodsService.clearCart(userId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Корзина пользователя #" + userId + " очищена");
        return ResponseEntity.ok(msg);
    }

    @DeleteMapping("/{userId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить пользователя", description = "Полностью удаляет пользователя из системы")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long userId) {
        log.debug("DELETE admin user {}", userId);
        authService.deleteUser(userId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Пользователь #" + userId + " удален");
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<Object> getUserOrders(@PathVariable Long userId) {
        log.debug("GET admin user {} orders", userId);
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }
}
