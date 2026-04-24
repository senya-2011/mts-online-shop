package com.mts.online_shop.service;

import com.mts.online_shop.exception.ProductNotInCartException;
import com.mts.online_shop.exception.ProductNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.model.CartWithUserResponse;
import com.mts.online_shop.model.ProductEntity;
import com.mts.online_shop.model.User;
import com.mts.online_shop.model.UserItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import com.mts.online_shop.repository.GoodsRepository;
import com.mts.online_shop.repository.OrderItemRepository;
import com.mts.online_shop.repository.UserItemRepository;
import com.mts.online_shop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;

@Service
public class GoodsService {

    private static final Logger log = LoggerFactory.getLogger(GoodsService.class);
    private final GoodsRepository goodsRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public GoodsService(GoodsRepository goodsRepository, UserItemRepository userItemRepository, 
                        UserRepository userRepository, OrderItemRepository orderItemRepository) {
        this.goodsRepository = goodsRepository;
        this.userItemRepository = userItemRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<ProductEntity> findAllGoods() {
        log.debug("findAllGoods (excluding deleted)");
        return goodsRepository.findAll().stream()
                .filter(product -> !Boolean.TRUE.equals(product.getDeleted()))
                .toList();
    }

    public List<ProductEntity> findAllGoodsIncludingDeleted() {
        log.debug("findAllGoods (including deleted)");
        return goodsRepository.findAll();
    }

    public Page<ProductEntity> findProducts(Pageable pageable, String search) {
        log.debug("findProducts page={} size={} search={}", pageable.getPageNumber(), pageable.getPageSize(), search);
        Page<ProductEntity> products;
        if (search != null && !search.isBlank()) {
            products = goodsRepository.findByNameContainingIgnoreCaseAndDeletedFalse(search.trim(), pageable);
        } else {
            products = goodsRepository.findByDeletedFalse(pageable);
        }
        return products;
    }

    public ProductEntity getProductById(Long productId) {
        ProductEntity product = goodsRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new ProductNotFoundException("Product with id: " + productId + " has been deleted");
        }
        return product;
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

    public List<UserItem> getAllCarts() {
        return userItemRepository.findAll();
    }

    public List<CartWithUserResponse> getAllCartsWithUsers() {
        log.debug("getAllCartsWithUsers (admin)");
        List<UserItem> allItems = userItemRepository.findAll();
        
        // Группируем по пользователям
        Map<Long, CartWithUserResponse> userCarts = new HashMap<>();
        
        for (UserItem item : allItems) {
            Long userId = item.getUser().getId();
            CartWithUserResponse cart = userCarts.computeIfAbsent(userId, k -> {
                CartWithUserResponse newCart = new CartWithUserResponse();
                newCart.setUserId(userId);
                newCart.setUsername(item.getUser().getLogin());
                newCart.setEmail(item.getUser().getEmail());
                newCart.setItems(new ArrayList<>());
                newCart.setTotalPrice(0.0);
                return newCart;
            });
            
            // Добавляем товар в корзину пользователя
            CartWithUserResponse.CartItemResponse cartItem = new CartWithUserResponse.CartItemResponse();
            cartItem.setItemId(item.getId());
            cartItem.setProductId(item.getProduct().getId());
            cartItem.setProductName(item.getProduct().getName());
            cartItem.setProductPrice(item.getProduct().getPrice().doubleValue());
            cartItem.setQuantity(item.getQuantity());
            cartItem.setSubtotal(item.getProduct().getPrice().doubleValue() * item.getQuantity());
            
            cart.getItems().add(cartItem);
            cart.setTotalPrice(cart.getTotalPrice() + cartItem.getSubtotal());
        }
        
        return new ArrayList<>(userCarts.values());
    }

    // ===== АДМИН-МЕТОДЫ для управления товарами =====

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ProductEntity createProduct(String name, BigDecimal price) {
        log.info("createProduct name={} price={}", name, price);
        ProductEntity product = new ProductEntity(name, price);
        ProductEntity saved = goodsRepository.save(product);
        log.info("Product created id={}", saved.getId());
        return saved;
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ProductEntity updateProduct(Long productId, String name, BigDecimal price) {
        log.info("updateProduct id={}", productId);
        ProductEntity product = goodsRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));
        
        if (name != null) product.setName(name);
        if (price != null) product.setPrice(price);
        
        ProductEntity saved = goodsRepository.save(product);
        log.info("Product updated id={}", saved.getId());
        return saved;
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        log.info("deleteProduct id={}", productId);
        ProductEntity product = goodsRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " not found"));
        
        // Проверяем, используется ли товар в заказах
        boolean usedInOrders = orderItemRepository.existsByProductId(productId);
        
        if (usedInOrders) {
            // Мягкое удаление - помечаем как deleted
            product.setDeleted(true);
            goodsRepository.save(product);
            log.info("Product marked as deleted (soft delete) id={}", productId);
        } else {
            // Физическое удаление - товар не используется в заказах
            goodsRepository.delete(product);
            log.info("Product deleted permanently (hard delete) id={}", productId);
        }
    }
}
