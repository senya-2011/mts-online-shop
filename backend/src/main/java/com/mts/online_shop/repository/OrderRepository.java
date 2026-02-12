package com.mts.online_shop.repository;

import com.mts.online_shop.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {
    Optional<Order> getOrderById(Long id);
    List<Order> getOrdersByUserId(Long userId);
}
