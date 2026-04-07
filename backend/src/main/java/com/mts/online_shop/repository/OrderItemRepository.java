package com.mts.online_shop.repository;

import com.mts.online_shop.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByProductId(Long productId);

    boolean existsByProductId(Long productId);
}
