package com.mts.online_shop.controller;

import com.mts.online_shop.api.OrdersApi;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.model.OrderRequest;
import com.mts.online_shop.model.OrderResponse;
import com.mts.online_shop.model.PaymentRequest;
import com.mts.online_shop.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController implements OrdersApi {

    private final OrderService orderService;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
    }

    //GET
    @Override
    public ResponseEntity<OrderResponse> getOrderById(Long orderId) {
        OrderResponse order = orderService.getOrderByOrderId(orderId);
        return ResponseEntity.ok(order);
    }

    //GET
    @Override
    public ResponseEntity<List<OrderResponse>> getUserOrders(Long userId) {
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    //POST
    @Override
    public ResponseEntity<OrderResponse> createOrder(OrderRequest orderRequest) {
        OrderResponse order = orderService.createOrder(orderRequest);
        return ResponseEntity.ok(order);
    }



    @Override
    public ResponseEntity<OrderResponse> payOrder(Long orderId, PaymentRequest paymentRequest) {
        return null;
    }
}
