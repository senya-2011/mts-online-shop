package com.mts.online_shop.controller;

import com.mts.online_shop.model.Order;
import com.mts.online_shop.model.PaymentRequest;
import com.mts.online_shop.service.TransactionalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactional Operations", description = "Управление транзакционными операциями")
@SecurityRequirement(name = "basicAuth")
public class TransactionalOrderController {

    private final TransactionalOrderService transactionalOrderService;

    public TransactionalOrderController(TransactionalOrderService transactionalOrderService) {
        this.transactionalOrderService = transactionalOrderService;
    }

    @PostMapping("/orders/create")
    @Operation(summary = "Создать заказ с оплатой", description = "Создает заказ и обрабатывает платеж в одной транзакции")
    @PreAuthorize("hasAuthority('PRIVILEGE_WRITE_ORDERS') and hasAuthority('PRIVILEGE_PROCESS_PAYMENTS')")
    public ResponseEntity<Order> createOrderWithPayment(
            @Parameter(description = "ID пользователя") @RequestParam Long userId,
            @RequestBody PaymentRequest paymentRequest) {
        try {
            Order order = transactionalOrderService.createOrderWithPayment(userId, paymentRequest);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Отменить заказ с возвратом", description = "Отменяет заказ и выполняет возврат в одной транзакции")
    @PreAuthorize("hasAuthority('PRIVILEGE_CANCEL_ORDERS')")
    public ResponseEntity<Void> cancelOrderWithRefund(
            @Parameter(description = "ID заказа") @PathVariable Long orderId) {
        try {
            transactionalOrderService.cancelOrderWithRefund(orderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Получить заказ", description = "Получает информацию о заказе в режиме чтения")
    @PreAuthorize("hasAuthority('PRIVILEGE_READ_ORDERS')")
    public ResponseEntity<Order> getOrder(
            @Parameter(description = "ID заказа") @PathVariable Long orderId) {
        Optional<Order> order = transactionalOrderService.getOrderById(orderId);
        return order.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
