package com.mts.online_shop.controller;

import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.OrderListResponse;
import com.mts.online_shop.model.OrderResponse;
import com.mts.online_shop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin-Cart", description = "Управление заказами администратора")
public class AdminCartController {

    private static final Logger log = LoggerFactory.getLogger(AdminCartController.class);
    private final OrderService orderService;

    public AdminCartController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders/{orderId}/cancel")
    @io.swagger.v3.oas.annotations.Operation(summary = "Отменить заказ (админ)", description = "Транзакционная отмена заказа администратором с автоматическим возвратом денег")
    public ResponseEntity<MessageResponse> cancelOrder(@PathVariable Long orderId) {
        log.debug("POST admin cancel order id={}", orderId);
        orderService.adminCancelOrder(orderId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Заказ #" + orderId + " отменен администратором");
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/orders")
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить заказы (админ)", description = "Возвращает список всех заказов для администратора")
    public ResponseEntity<OrderListResponse> getOrders() {
        log.debug("GET admin orders");
        // Получаем все заказы всех пользователей
        List<OrderResponse> allOrders = orderService.getAllOrders();
        OrderListResponse response = new OrderListResponse();
        response.setItems(allOrders);
        response.setTotal((long) allOrders.size());
        response.setPage(0);
        response.setSize(20);
        return ResponseEntity.ok(response);
    }
}
