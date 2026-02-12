package com.mts.online_shop.controller;

import com.mts.online_shop.api.GoodsApi;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.service.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GoodsController implements GoodsApi {

    private static final Logger log = LoggerFactory.getLogger(GoodsController.class);
    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @Override
    public ResponseEntity<List<Product>> getAllGoods() {
        log.debug("GET all goods");
        return ResponseEntity.ok(goodsService.findAllGoods());
    }

    @Override
    public ResponseEntity<List<Product>> getUserGoods(Long userId) {
        log.info("GET user goods userId={}", userId);
        return ResponseEntity.ok(goodsService.findUserGoods(userId));
    }

    @Override
    public ResponseEntity<Product> addProductToCart(Long userId, Long productId) {
        log.info("POST addProductToCart userId={} productId={}", userId, productId);
        return ResponseEntity.ok(goodsService.addProductInUserCart(userId, productId));
    }

    @Override
    public ResponseEntity<String> clearCart(Long userId) {
        log.info("POST clearCart userId={}", userId);
        goodsService.clearCart(userId);
        return ResponseEntity.ok("Корзина очищена");
    }

    @Override
    public ResponseEntity<String> deleteItemFromCart(Long userId, Long itemId) {
        log.info("DELETE deleteItemFromCart userId={} itemId={}", userId, itemId);
        goodsService.deleteProductFromCart(userId, itemId);
        return ResponseEntity.ok("Товар удален из корзины");
    }


}
