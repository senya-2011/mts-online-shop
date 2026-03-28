package com.mts.online_shop.controller;

import com.mts.online_shop.api.CartApi;
import com.mts.online_shop.mapper.ProductMapper;
import com.mts.online_shop.model.*;
import com.mts.online_shop.security.CurrentUserService;
import com.mts.online_shop.service.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@SecurityRequirement(name = "basicAuth")
public class CartController implements CartApi {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    private final GoodsService goodsService;
    private final ProductMapper productMapper;
    private final CurrentUserService currentUserService;

    public CartController(GoodsService goodsService, ProductMapper productMapper, CurrentUserService currentUserService) {
        this.goodsService = goodsService;
        this.productMapper = productMapper;
        this.currentUserService = currentUserService;
    }

    @Override
    public ResponseEntity<CartResponse> getCart() {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.debug("GET cart userId={}", userId);
        CartResponse response = new CartResponse();
        response.setItems(goodsService.getCartItems(userId).stream()
                .map(item -> {
                    CartItem ci = new CartItem();
                    ci.setItemId(item.getId());
                    ci.setProduct(productMapper.toDto(item.getProduct()));
                    return ci;
                })
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MessageResponse> clearCart() {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.info("DELETE clear cart userId={}", userId);
        goodsService.clearCart(userId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Корзина успешно очищена");
        return ResponseEntity.ok(msg);
    }

    @Override
    public ResponseEntity<MessageResponse> addCartItem(AddCartItemRequest addCartItemRequest) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        Long productId = addCartItemRequest.getProductId();
        log.info("POST add cart item userId={} productId={}", userId, productId);
        goodsService.addProductInUserCart(userId, productId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Товар добавлен в корзину");
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @Override
    public ResponseEntity<MessageResponse> removeCartItem(Long itemId) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.info("DELETE cart item userId={} itemId={}", userId, itemId);
        goodsService.removeCartItem(userId, itemId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Позиция удалена");
        return ResponseEntity.ok(msg);
    }
}
