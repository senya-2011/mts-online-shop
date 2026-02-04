package com.mts.online_shop.repository;

import com.mts.online_shop.model.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    List<UserItem> findByUser_Id(Long userId);
}
