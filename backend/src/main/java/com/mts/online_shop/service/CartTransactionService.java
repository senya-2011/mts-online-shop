package com.mts.online_shop.service;

import com.mts.online_shop.exception.ProductNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.model.UserItem;
import com.mts.online_shop.repository.UserItemRepository;
import com.mts.online_shop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartTransactionService {

    private static final Logger log = LoggerFactory.getLogger(CartTransactionService.class);
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final GoodsService goodsService;

    public CartTransactionService(UserItemRepository userItemRepository, 
                                 UserRepository userRepository,
                                 GoodsService goodsService) {
        this.userItemRepository = userItemRepository;
        this.userRepository = userRepository;
        this.goodsService = goodsService;
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = {UserNotFoundException.class, RuntimeException.class})
    public void clearCartCompletely(Long userId) {
        log.info("clearCartCompletely userId={}", userId);
        
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        
        List<UserItem> cartItems = userItemRepository.findByUser_Id(userId);
        
        if (!cartItems.isEmpty()) {
            userItemRepository.deleteAllByUser_Id(userId);
            log.info("Cleared {} items from cart for user {}", cartItems.size(), userId);
        }
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = {UserNotFoundException.class, RuntimeException.class})
    public void removeMultipleItems(Long userId, List<Long> itemIds) {
        log.info("removeMultipleItems userId={} items={}", userId, itemIds.size());
        
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        
        List<UserItem> itemsToRemove = userItemRepository.findByUser_Id(userId).stream()
                .filter(item -> itemIds.contains(item.getId()))
                .toList();
        
        if (itemsToRemove.size() != itemIds.size()) {
            log.warn("Some items not found in cart for user {}. Requested: {}, Found: {}", 
                    userId, itemIds.size(), itemsToRemove.size());
        }
        
        userItemRepository.deleteAll(itemsToRemove);
        log.info("Removed {} items from cart for user {}", itemsToRemove.size(), userId);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void mergeCarts(Long targetUserId, Long sourceUserId) {
        log.info("mergeCarts targetUserId={} sourceUserId={}", targetUserId, sourceUserId);
        
        if (targetUserId.equals(sourceUserId)) {
            log.warn("Attempted to merge cart with same user {}", targetUserId);
            return;
        }
        
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("Target user not found: " + targetUserId));
        userRepository.findById(sourceUserId)
                .orElseThrow(() -> new UserNotFoundException("Source user not found: " + sourceUserId));
        
        List<UserItem> sourceItems = userItemRepository.findByUser_Id(sourceUserId);
        
        for (UserItem sourceItem : sourceItems) {
            if (!userItemRepository.existsByUser_IdAndProduct_Id(targetUserId, sourceItem.getProduct().getId())) {
                UserItem newItem = new UserItem();
                newItem.setUser(userRepository.getReferenceById(targetUserId));
                newItem.setProduct(sourceItem.getProduct());
                userItemRepository.save(newItem);
            }
        }
        
        userItemRepository.deleteAllByUser_Id(sourceUserId);
        log.info("Merged {} items from user {} to user {}", 
                sourceItems.size(), sourceUserId, targetUserId);
    }
}
