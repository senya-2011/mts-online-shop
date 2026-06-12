package com.mts.online_shop.repository;

import com.mts.online_shop.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithItemsAndProducts(@Param("id") Long id);

    Optional<Order> getOrderById(Long id);

    List<Order> getOrdersByUserId(Long userId);

    Page<Order> findByUser_Id(Long userId, Pageable pageable);
}
