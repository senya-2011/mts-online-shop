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
    @io.swagger.v3.oas.annotations.Operation(
        summary = "Get orders of user", 
        description = "Returns list of all orders of current user with pagination support"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
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
    @io.swagger.v3.oas.annotations.Operation(
        summary = "Get order by ID", 
        description = "Returns detailed information about specific order. User can only access their own orders."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - order does not belong to current user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
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
    @io.swagger.v3.oas.annotations.Operation(
        summary = "Cancel order with refund", 
        description = "Transactional order cancellation with automatic refund to user account via bank integration"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled and refunded successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error during cancellation")
    })
    public ResponseEntity<MessageResponse> cancelOrder(@PathVariable Long orderId) {
        Long userId = currentUserService.getCurrentUserIdOrThrow();
        orderService.cancelOrder(orderId, userId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Заказ #" + orderId + " отменен, деньги возвращены");
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/create")
    @io.swagger.v3.oas.annotations.Operation(
        summary = "Create order with payment", 
        description = "Creates an order from user's cart and processes payment via bank integration. Payment data is required in request body."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order created and paid successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - empty cart, invalid payment data, or missing payment information"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> createOrderWithPayment(@io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Payment information for order processing",
        required = true,
        content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PaymentRequest.class),
            examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Payment Example",
                    value = "{\"cardNumber\":\"1234567890123456\",\"cvv\":\"123\",\"expiresAt\":\"12/25\"}",
                    description = "Example payment data"
                )
            }
        )
    ) @RequestBody PaymentRequest paymentRequest) {
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
            
            // Validate payment data
            String cardNumber = paymentRequest.getCardNumber();
            String cvv = paymentRequest.getCvv();
            String expiresAt = paymentRequest.getExpiresAt();
            
            log.info("Received payment data from request:");
            log.info("  Card: ****-****-****-{}", maskCardNumber(cardNumber));
            log.info("  Expires: {}", expiresAt);
            log.info("  CVV: ***");
            
            // Validate required fields
            if (cardNumber == null || cardNumber.trim().isEmpty()) {
                log.error("Card number is required for payment");
                return ResponseEntity.badRequest().build();
            }
            if (cvv == null || cvv.trim().isEmpty()) {
                log.error("CVV is required for payment");
                return ResponseEntity.badRequest().build();
            }
            if (expiresAt == null || expiresAt.trim().isEmpty()) {
                log.error("Expiry date is required for payment");
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Processing payment via real bank:");
            log.info("  Card: ****-****-****-{}", cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
            log.info("  Amount: ${}", totalAmount);
            
            // Используем OrderService для создания заказа с реальной оплатой через банк
            OrderResponse orderResponse = orderService.createOrderWithPayment(userId, paymentRequest);
            
            log.info("Order created and paid successfully via real bank");
            
            return ResponseEntity.ok(orderResponse);
            
        } catch (EmptyCartException e) {
            log.error("Empty cart error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (org.springframework.http.converter.HttpMessageNotReadableException e) {
            log.error("Missing payment data in request body");
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
