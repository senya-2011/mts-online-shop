package com.mts.online_shop.service;

import com.mts.online_shop.exception.EmptyCartException;
import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.model.*;
import com.mts.online_shop.repository.OrderRepository;
import com.mts.online_shop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final GoodsService goodsService;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper,
                        GoodsService goodsService,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.goodsService = goodsService;
        this.userRepository = userRepository;
    }

    public OrderResponse getOrderByOrderId(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Long userId = orderRequest.getUserId();
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        List<Product> productsInCart = goodsService.findUserGoods(userId);
        if (productsInCart.isEmpty()) {
            throw new EmptyCartException("Cart for user with id: " + userId + " is empty");
        }

        Order order = orderMapper.toOrder(orderRequest);

        List<OrderItem> orderItems = orderMapper.toOrderItems(order, productsInCart);

        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        goodsService.clearCart(userId);

        return orderMapper.toOrderResponse(savedOrder);
    }
}
