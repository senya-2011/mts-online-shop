package com.mts.online_shop.service;

import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.model.Order;
import com.mts.online_shop.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order getOrderByOrderId(Long orderId){
        return orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
    }


}
