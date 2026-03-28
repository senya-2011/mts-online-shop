package com.mts.online_shop.controller;

import com.mts.online_shop.api.OrdersApi;
import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.OrderListResponse;
import com.mts.online_shop.model.OrderResponse;
import com.mts.online_shop.model.PaymentRequest;
import com.mts.online_shop.security.CurrentUserService;
import com.mts.online_shop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
public class OrderController implements OrdersApi {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @Override
    public ResponseEntity<OrderResponse> getOrderById(Long orderId) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        OrderResponse order = orderService.getOrderByOrderId(orderId, userId);
        return ResponseEntity.ok(order);
    }

    @Override
    public ResponseEntity<OrderListResponse> getOrders() {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        OrderListResponse response = new OrderListResponse();
        response.setItems(orderService.getOrdersByUserId(userId));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MessageResponse> createOrder() {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        Long orderId = orderService.createOrder(userId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Заказ #" + orderId + " создан");
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @Override
    public ResponseEntity<MessageResponse> payOrder(Long orderId, PaymentRequest paymentRequest) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        orderService.payOrder(orderId, paymentRequest, userId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Оплата прошла успешно");
        return ResponseEntity.ok(msg);
    }
}
