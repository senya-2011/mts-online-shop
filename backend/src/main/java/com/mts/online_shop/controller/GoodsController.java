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

    //GET
    @Override
    public ResponseEntity<List<Product>> getAllGoods() {
        return ResponseEntity.ok(goodsService.findAllGoods());
    }
    //GET
    @Override
    public ResponseEntity<List<Product>> getUserGoods(Long userId) {
        return ResponseEntity.ok(goodsService.findUserGoods(userId));
    }

    //POST
    @Override
    public ResponseEntity<Product> addProductToCart(Long userId, Long productId) {
        return ResponseEntity.ok(goodsService.addProductInUserCart(userId, productId));
    }

    //DELETE
    @Override
    public ResponseEntity<String> clearCart(Long userId) {
        goodsService.clearCart(userId);
        return ResponseEntity.ok("Корзина очищена");
    }

    //DELETE
    @Override
    public ResponseEntity<String> deleteItemFromCart(Long userId, Long itemId) {
        goodsService.deleteProductFromCart(userId, itemId);
        return ResponseEntity.ok("Товар удален из корзины");
    }


}
