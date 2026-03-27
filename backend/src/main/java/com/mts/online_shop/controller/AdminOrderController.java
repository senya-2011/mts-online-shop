package com.mts.online_shop.controller;

import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.OrderResponse;
import com.mts.online_shop.model.OrderListResponse;
import com.mts.online_shop.security.annotation.RequirePrivilege;
import com.mts.online_shop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @RequirePrivilege("ORDER_VIEW_ALL")
    public ResponseEntity<OrderListResponse> getAllOrders() {
        log.info("GET all orders (admin)");
        // В реальном приложении здесь был бы метод для получения всех заказов
        OrderListResponse response = new OrderListResponse();
        response.setItems(List.of()); // Заглушка
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/process")
    @RequirePrivilege("ORDER_PROCESS")
    public ResponseEntity<MessageResponse> processOrder(@PathVariable Long id) {
        log.info("PUT process order {} (admin)", id);
        // Логика обработки заказа
        MessageResponse response = new MessageResponse();
        response.setMessage("Заказ #" + id + " обработан");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @RequirePrivilege("ORDER_MANAGE_ALL")
    public ResponseEntity<MessageResponse> cancelOrder(@PathVariable Long id) {
        log.info("POST cancel order {} (admin)", id);
        // Логика отмены заказа
        MessageResponse response = new MessageResponse();
        response.setMessage("Заказ #" + id + " отменен");
        return ResponseEntity.ok(response);
    }
}
