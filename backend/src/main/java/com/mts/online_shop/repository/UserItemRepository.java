package com.mts.online_shop.repository;

import com.mts.online_shop.model.User;
import com.mts.online_shop.model.UserItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    List<UserItem> findByUser_Id(Long userId);

    Page<UserItem> findByUser_Id(Long userId, Pageable pageable);
    java.util.Optional<UserItem> findByIdAndUser_Id(Long itemId, Long userId);
    void deleteAllByUser(User user);
    void deleteByUser_IdAndProduct_Id(Long userId, Long productId);
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);
}
