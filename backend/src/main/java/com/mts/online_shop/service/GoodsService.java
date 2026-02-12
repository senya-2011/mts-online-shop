package com.mts.online_shop.service;

import com.mts.online_shop.exception.ProductNotInCartException;
import com.mts.online_shop.exception.ProductNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.model.User;
import com.mts.online_shop.model.UserItem;
import com.mts.online_shop.repository.GoodsRepository;
import com.mts.online_shop.repository.UserItemRepository;
import com.mts.online_shop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoodsService {

    private static final Logger log = LoggerFactory.getLogger(GoodsService.class);
    private final GoodsRepository goodsRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;

    public GoodsService(GoodsRepository goodsRepository, UserItemRepository userItemRepository, UserRepository userRepository) {
        this.goodsRepository = goodsRepository;
        this.userItemRepository = userItemRepository;
        this.userRepository = userRepository;
    }

    public List<Product> findAllGoods() {
        log.debug("findAllGoods");
        return goodsRepository.findAll();
    }

    public List<Product> findUserGoods(Long userId) {
        log.debug("findUserGoods userId={}", userId);
        userRepository.findById(userId).
                orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        return userItemRepository.findByUser_Id(userId).stream()
                .map(UserItem::getProduct)
                .toList();
    }

    public Product addProductInUserCart(Long userId, Long productId) {
        log.info("addProductInUserCart userId={} productId={}", userId, productId);
        User user = userRepository.findById(userId).
                orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        Product product = goodsRepository.findById(productId).
                orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));
        userItemRepository.save(new UserItem(user, product));
        return product;
    }

    @Transactional
    public void deleteProductFromCart(Long userId, Long productId) {
        log.info("deleteProductFromCart userId={} productId={}", userId, productId);
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        goodsRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));
        if (!userItemRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            throw new ProductNotInCartException("Product with id: " + productId + " is not in cart");
        }
        userItemRepository.deleteByUser_IdAndProduct_Id(userId, productId);
    }

    @Transactional
    public void clearCart(Long userId) {
        log.debug("clearCart userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        userItemRepository.deleteAllByUser(user);
    }
}
