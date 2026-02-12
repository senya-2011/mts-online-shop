package com.mts.online_shop.controller;

import com.mts.online_shop.api.OrdersApi;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.model.OrderRequest;
import com.mts.online_shop.model.OrderResponse;
import com.mts.online_shop.model.PaymentRequest;
import com.mts.online_shop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController implements OrdersApi {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
    }

    //GET
    @Override
    public ResponseEntity<OrderResponse> getOrderById(Long orderId) {
        log.info("GET order id={}", orderId);
        OrderResponse order = orderService.getOrderByOrderId(orderId);
        return ResponseEntity.ok(order);
    }

    @Override
    public ResponseEntity<List<OrderResponse>> getUserOrders(Long userId) {
        log.info("GET user orders userId={}", userId);
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(OrderRequest orderRequest) {
        log.info("POST create order userId={}", orderRequest.getUserId());
        OrderResponse order = orderService.createOrder(orderRequest);
        return ResponseEntity.ok(order);
    }

    @Override
    public ResponseEntity<OrderResponse> payOrder(Long orderId, PaymentRequest paymentRequest) {
        log.info("POST pay order id={}", orderId);
        OrderResponse order = orderService.payOrder(orderId, paymentRequest);
        return ResponseEntity.ok(order);
    }
}
