package com.mts.online_shop.service;

import com.mts.online_shop.exception.ProductNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.model.User;
import com.mts.online_shop.model.UserItem;
import com.mts.online_shop.repository.GoodsRepository;
import com.mts.online_shop.repository.UserItemRepository;
import com.mts.online_shop.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {
    private final GoodsRepository goodsRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;

    public GoodsService(GoodsRepository goodsRepository, UserItemRepository userItemRepository, UserRepository userRepository) {
        this.goodsRepository = goodsRepository;
        this.userItemRepository = userItemRepository;
        this.userRepository = userRepository;
    }

    public List<Product> findAllGoods() {
        return goodsRepository.findAll();
    }

    public List<Product> findUserGoods(Long userId) {
        userRepository.findById(userId).
                orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        return userItemRepository.findByUser_Id(userId).stream()
                .map(UserItem::getProduct)
                .toList();
    }

    public Product addProductInUserCart(Long userId, Long productId) {
        User user = userRepository.findById(userId).
                orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        Product product = goodsRepository.findById(productId).
                orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));
        userItemRepository.save(new UserItem(user, product));
        return product;
    }
}
