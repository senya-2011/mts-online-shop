package com.mts.online_shop.controller;

import com.mts.online_shop.model.CartResponse;
import com.mts.online_shop.model.CartItem;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.model.AddCartItemRequest;
import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.security.CurrentUserService;
import com.mts.online_shop.service.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('USER')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "User-Cart", description = "Управление корзиной пользователя")
public class UserCartController {

    private static final Logger log = LoggerFactory.getLogger(UserCartController.class);
    private final GoodsService goodsService;
    private final CurrentUserService currentUserService;

    public UserCartController(GoodsService goodsService, CurrentUserService currentUserService) {
        this.goodsService = goodsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить корзину пользователя", description = "Возвращает текущее содержимое корзины авторизованного пользователя")
    public ResponseEntity<CartResponse> getCart() {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.debug("GET cart for user id={}", userId);
        
        // Получаем реальные товары из корзины
        var cartItems = goodsService.getCartItems(userId);
        
        // Конвертируем в CartResponse с реальными товарами
        CartResponse cart = new CartResponse();
        cart.setItems(cartItems.stream()
                .map(item -> {
                    CartItem cartItem = new CartItem();
                    cartItem.setId(item.getId());
                    cartItem.setQuantity(item.getQuantity());
                    
                    Product product = new Product();
                    product.setId(item.getProduct().getId());
                    product.setName(item.getProduct().getName());
                    product.setPrice(item.getProduct().getPrice().doubleValue());
                    cartItem.setProduct(product);
                    
                    return cartItem;
                })
                .toList());
        
        // Считаем реальную сумму
        long total = cartItems.stream()
                .mapToLong(item -> item.getProduct().getPrice().longValue() * item.getQuantity())
                .sum();
        cart.setTotal(total);
        
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Очистить корзину", description = "Удаляет все товары из корзины текущего пользователя")
    public ResponseEntity<MessageResponse> clearCart() {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.debug("DELETE cart for user id={}", userId);
        
        try {
            // Очищаем корзину через GoodsService
            goodsService.clearCart(userId);
            
            MessageResponse msg = new MessageResponse();
            msg.setMessage("Корзина очищена");
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            log.error("Error clearing cart: {}", e.getMessage());
            MessageResponse msg = new MessageResponse();
            msg.setMessage("Ошибка при очистке корзины: " + e.getMessage());
            return ResponseEntity.badRequest().body(msg);
        }
    }

    @DeleteMapping("/items/{itemId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить товар из корзины", description = "Удаляет конкретный товар из корзины по его ID")
    public ResponseEntity<MessageResponse> removeItem(@PathVariable Long itemId) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.debug("DELETE cart item {} for user id={}", itemId, userId);
        
        try {
            // Удаляем товар из корзины через GoodsService
            goodsService.removeCartItem(userId, itemId);
            
            MessageResponse msg = new MessageResponse();
            msg.setMessage("Товар #" + itemId + " удален из корзины");
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            log.error("Error removing cart item: {}", e.getMessage());
            MessageResponse msg = new MessageResponse();
            msg.setMessage("Ошибка при удалении товара: " + e.getMessage());
            return ResponseEntity.badRequest().body(msg);
        }
    }

    @PostMapping("/items")
    @io.swagger.v3.oas.annotations.Operation(summary = "Добавить товар в корзину", description = "Добавляет указанный товар в корзину текущего пользователя")
    public ResponseEntity<MessageResponse> addToCart(@RequestBody AddCartItemRequest request) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.debug("POST add to cart user={} productId={}", userId, request.getProductId());
        
        try {
            // Добавляем товар в корзину через GoodsService
            goodsService.addProductInUserCart(userId, request.getProductId());
            
            MessageResponse msg = new MessageResponse();
            msg.setMessage("Товар #" + request.getProductId() + " добавлен в корзину");
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            log.error("Error adding product to cart: {}", e.getMessage());
            MessageResponse msg = new MessageResponse();
            msg.setMessage("Ошибка при добавлении товара: " + e.getMessage());
            return ResponseEntity.badRequest().body(msg);
        }
    }
}
