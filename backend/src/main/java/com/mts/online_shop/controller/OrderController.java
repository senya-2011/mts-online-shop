package com.mts.online_shop.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.mts.online_shop.exception.EmptyCartException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.mapper.ProductMapper;
import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.OrderListResponse;
import com.mts.online_shop.model.OrderResponse;
import com.mts.online_shop.model.PaymentRequest;
import com.mts.online_shop.model.Order;
import com.mts.online_shop.repository.OrderRepository;
import com.mts.online_shop.security.CurrentUserService;
import com.mts.online_shop.service.OrderService;
import com.mts.online_shop.service.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "jwtAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Order", description = "Управление заказами")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;
    private final CurrentUserService currentUserService;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final OrderRepository orderRepository;
    private final GoodsService goodsService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService,
                           OrderMapper orderMapper, ProductMapper productMapper, 
                           OrderRepository orderRepository, GoodsService goodsService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.orderRepository = orderRepository;
        this.goodsService = goodsService;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить заказы пользователя", description = "Возвращает список всех заказов текущего пользователя")
    public ResponseEntity<OrderListResponse> getOrders(@RequestParam(required = false) Integer page, 
                                                      @RequestParam(required = false) Integer size) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        OrderListResponse response = new OrderListResponse();
        response.setItems(orders);
        response.setTotal((long) orders.size());
        response.setPage(page != null ? page : 0);
        response.setSize(size != null ? size : 20);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить заказ по ID", description = "Возвращает детальную информацию о конкретном заказе")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        
        // Получаем заказ из базы данных
        Optional<Order> orderOptional = orderRepository.getOrderById(orderId);
        if (orderOptional.isEmpty()) {
            log.error("Order not found with ID: {}", orderId);
            return ResponseEntity.notFound().build();
        }
        
        Order order = orderOptional.get();
        
        // Проверяем, что заказ принадлежит текущему пользователю
        if (!order.getUser().getId().equals(userId)) {
            log.error("Order {} does not belong to user {}", orderId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Конвертируем в OrderResponse
        OrderResponse orderResponse = orderMapper.toOrderResponse(order, productMapper);
        if (orderResponse == null) {
            log.error("Failed to map order {} to OrderResponse", orderId);
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Retrieved order {} for user {}", orderId, userId);
        return ResponseEntity.ok(orderResponse);
    }

    @PostMapping("/{orderId}/cancel")
    @io.swagger.v3.oas.annotations.Operation(summary = "Отменить заказ с возвратом", description = "Транзакционная отмена заказа с автоматическим возвратом денег на счет пользователя")
    public ResponseEntity<MessageResponse> cancelOrder(@PathVariable Long orderId) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        orderService.cancelOrder(orderId, userId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Заказ #" + orderId + " отменен, деньги возвращены");
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/create")
    @io.swagger.v3.oas.annotations.Operation(summary = "Создать заказ с оплатой", description = "Транзакционное создание заказа на основе корзины пользователя с оплатой по данным карты")
    public ResponseEntity<OrderResponse> createOrderWithPayment(@RequestBody PaymentRequest paymentRequest) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        log.debug("POST create order with payment for user id={}", userId);
        log.debug("Payment data: card={}, expiry={}, cvv={}", 
            maskCardNumber(paymentRequest.getCardNumber()), 
            paymentRequest.getExpiresAt(), 
            "***");
        
        try {
            log.info("Creating order with real bank payment using OrderService");
            log.info("User ID: {}", userId);
            
            // Проверим корзину перед созданием заказа
            var cartItems = goodsService.getCartItems(userId);
            log.info("Cart has {} items for user {}", cartItems.size(), userId);
            
            if (cartItems.isEmpty()) {
                log.error("Cannot create order - cart is empty for user {}", userId);
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Cart items: {}", cartItems.stream().map(item -> item.getProduct().getName()).toList());
            
            // Рассчитываем сумму заказа
            double totalAmount = cartItems.stream()
                    .mapToDouble(item -> item.getProduct().getPrice().doubleValue() * item.getQuantity())
                    .sum();
            log.info("Order total amount: ${}", totalAmount);
            
            // Подготовка данных карты из запроса (или используем тестовые)
            PaymentRequest paymentRequestForService = new PaymentRequest();
            String cardNumber = paymentRequest.getCardNumber();
            String cvv = paymentRequest.getCvv();
            String expiresAt = paymentRequest.getExpiresAt();
            
            log.info("Received payment data from request:");
            log.info("  Raw cardNumber: '{}' (length: {})", cardNumber, cardNumber != null ? cardNumber.length() : 0);
            log.info("  Raw cvv: '{}'", cvv);
            log.info("  Raw expiresAt: '{}'", expiresAt);
            
            // Если данные не предоставлены, используем тестовую карту из банка с балансом
            if (cardNumber == null || cardNumber.isEmpty()) {
                cardNumber = "1234123412341234";  // Карта с балансом $15000
                cvv = "444";  // CVV из базы
                expiresAt = "09/30";  // Срок из базы
                log.info("Using test card with balance: 1234123412341234 ($15000)");
            }
            
            paymentRequestForService.setCardNumber(cardNumber);
            paymentRequestForService.setCvv(cvv);
            paymentRequestForService.setExpiresAt(expiresAt);
            
            log.info("Processing payment via real bank:");
            log.info("  Card: ****-****-****-{}", cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
            log.info("  Amount: ${}", totalAmount);
            
            // Используем OrderService для создания заказа с реальной оплатой через банк
            OrderResponse orderResponse = orderService.createOrderWithPayment(userId, paymentRequestForService);
            
            log.info("Order created and paid successfully via real bank");
            
            return ResponseEntity.ok(orderResponse);
            
        } catch (EmptyCartException e) {
            log.error("Empty cart error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating order with payment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}
