package com.mts.online_shop.service;

import com.mts.online_shop.exception.ProductNotInCartException;
import com.mts.online_shop.exception.ProductNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.model.ProductEntity;
import com.mts.online_shop.model.User;
import com.mts.online_shop.model.UserItem;
import com.mts.online_shop.repository.GoodsRepository;
import com.mts.online_shop.repository.UserItemRepository;
import com.mts.online_shop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public List<ProductEntity> findAllGoods() {
        log.debug("findAllGoods");
        return goodsRepository.findAll();
    }

    public Page<ProductEntity> findProducts(Pageable pageable, String search) {
        log.debug("findProducts page={} size={} search={}", pageable.getPageNumber(), pageable.getPageSize(), search);
        if (search != null && !search.isBlank()) {
            return goodsRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        }
        return goodsRepository.findAll(pageable);
    }

    public ProductEntity getProductById(Long productId) {
        return goodsRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));
    }

    public List<ProductEntity> findUserGoods(Long userId) {
        log.debug("findUserGoods userId={}", userId);
        userRepository.findById(userId).
                orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        return userItemRepository.findByUser_Id(userId).stream()
                .map(UserItem::getProduct)
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = {UserNotFoundException.class, ProductNotFoundException.class, RuntimeException.class})
    public UserItem addProductInUserCart(Long userId, Long productId) {
        log.info("addProductInUserCart userId={} productId={}", userId, productId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        
        ProductEntity product = goodsRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));
        
        if (userItemRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            log.warn("Product {} already in cart for user {}", productId, userId);
            return userItemRepository.findByUser_IdAndProduct_Id(userId, productId)
                    .orElseThrow(() -> new RuntimeException("Unexpected error finding existing cart item"));
        }
        
        UserItem userItem = new UserItem(user, product);
        UserItem savedItem = userItemRepository.save(userItem);
        
        log.info("Product {} added to cart for user {}", productId, userId);
        return savedItem;
    }

    public List<UserItem> getCartItems(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        return userItemRepository.findByUser_Id(userId);
    }

    public Page<UserItem> getCartItemsPage(Long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        return userItemRepository.findByUser_Id(userId, pageable);
    }

    public UserItem getCartItem(Long userId, Long itemId) {
        return userItemRepository.findByIdAndUser_Id(itemId, userId)
                .orElseThrow(() -> new ProductNotInCartException("Cart item with id: " + itemId + " not found"));
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = {ProductNotInCartException.class, RuntimeException.class})
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

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void removeCartItem(Long userId, Long itemId) {
        UserItem item = getCartItem(userId, itemId);
        userItemRepository.delete(item);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        log.debug("clearCart userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        userItemRepository.deleteAllByUser(user);
    }
}
