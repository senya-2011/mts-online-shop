package com.mts.online_shop.repository;

import com.mts.online_shop.model.User;
import com.mts.online_shop.model.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    List<UserItem> findByUser_Id(Long userId);
    List<UserItem> findAllByUser_Id(Long userId);
    java.util.Optional<UserItem> findByIdAndUser_Id(Long itemId, Long userId);
    void deleteAllByUser(User user);
    void deleteAllByUser_Id(Long userId);
    void deleteByUser_IdAndProduct_Id(Long userId, Long productId);
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);
    java.util.Optional<UserItem> findByUser_IdAndProduct_Id(Long userId, Long productId);
}
