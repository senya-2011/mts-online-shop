package com.mts.online_shop.controller;

import com.mts.online_shop.api.GoodsApi;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.service.GoodsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GoodsController implements GoodsApi {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @Override
    public ResponseEntity<List<Product>> getAllGoods() {
        return ResponseEntity.ok(goodsService.findAllGoods());
    }

    @Override
    public ResponseEntity<Product> addProductToCart(Long userId, Long productId) {
        return null;
    }

    @Override
    public ResponseEntity<Void> clearCart(Long userId) {
        return null;
    }

    @Override
    public ResponseEntity<List<Product>> deleteItemFromCart(Long userId, Long itemId) {
        return null;
    }

    @Override
    public ResponseEntity<List<Product>> getUserGoods(Long userId) {
        return null;
    }
}
