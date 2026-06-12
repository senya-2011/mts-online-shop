package com.mts.online_shop.service;

import com.mts.online_shop.exception.EmptyCartException;
import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.exception.OrderAccessDeniedException;
import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.mapper.ProductMapper;
import com.mts.online_shop.client.bitrix.BitrixEisClientJcaAdapter;
import com.mts.online_shop.model.*;
import com.mts.online_shop.repository.OrderRepository;
import com.mts.online_shop.repository.UserRepository;
import com.mts.messaging.contracts.TelegramNotificationEnvelope;
import com.mts.online_shop.client.bank.BankClient;
import com.mts.online_shop.messaging.MqttNotificationPublisher;
import com.mts.online_shop.model.UserTelegramLink;
import com.mts.online_shop.repository.UserTelegramLinkRepository;
import com.mts.online_shop.simulator.mail.MailSimulator;
import com.mts.online_shop.model.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final GoodsService goodsService;
    private final UserRepository userRepository;
    private final BankClient bankClient;
    private final ProductReservationService productReservationService;
    private final MailSimulator mailSimulator;
    private final UserTelegramLinkRepository userTelegramLinkRepository;
    private final MqttNotificationPublisher mqttNotificationPublisher;
    private final ObjectProvider<BitrixEisClientJcaAdapter> bitrixEisClientProvider;

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper,
                        ProductMapper productMapper,
                        GoodsService goodsService,
                        UserRepository userRepository,
                        BankClient bankClient,
                        ProductReservationService productReservationService,
                        MailSimulator mailSimulator,
                        UserTelegramLinkRepository userTelegramLinkRepository,
                        MqttNotificationPublisher mqttNotificationPublisher,
                        ObjectProvider<BitrixEisClientJcaAdapter> bitrixEisClientProvider) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.goodsService = goodsService;
        this.userRepository = userRepository;
        this.bankClient = bankClient;
        this.productReservationService = productReservationService;
        this.mailSimulator = mailSimulator;
        this.userTelegramLinkRepository = userTelegramLinkRepository;
        this.mqttNotificationPublisher = mqttNotificationPublisher;
        this.bitrixEisClientProvider = bitrixEisClientProvider;
    }

    public com.mts.online_shop.model.OrderResponse getOrderByOrderId(Long orderId, Long currentUserId) {
        log.debug("getOrderByOrderId orderId={}", orderId);
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        if (!order.getUser().getId().equals(currentUserId)) {
            throw new OrderAccessDeniedException("Order does not belong to current user");
        }
        return orderMapper.toOrderResponse(order, productMapper);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = {EmptyCartException.class, UserNotFoundException.class, RuntimeException.class})
    public Long createOrder(Long userId) {
        log.info("createOrder userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        List<ProductEntity> productsInCart = goodsService.findUserGoods(userId);
        if (productsInCart.isEmpty()) {
            throw new EmptyCartException("Cart for user with id: " + userId + " is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.CREATED);
        List<OrderItem> orderItems = orderMapper.toOrderItems(order, productsInCart);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        goodsService.clearCart(userId);
        
        log.info("order created id={} for user={}", savedOrder.getId(), userId);
        return savedOrder.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createOrderFromCartPendingPayment(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        List<ProductEntity> productsInCart = goodsService.findUserGoods(userId);
        if (productsInCart.isEmpty()) {
            throw new EmptyCartException("Cart for user with id: " + userId + " is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        List<OrderItem> orderItems = orderMapper.toOrderItems(order, productsInCart);
        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        goodsService.clearCart(userId);
        return savedOrder.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureOrderPendingPayment(Long orderId, Long userId) {
        Order order = loadOrderForUser(orderId, userId);
        if (order.getStatus() == OrderStatus.CREATED) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            orderRepository.save(order);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidPaymentDataException("Order cannot be created from status: " + order.getStatus());
        }
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.debug("getOrdersByUserId userId={}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        List<Order> orders = orderRepository.getOrdersByUserId(userId);
        return orderMapper.toOrderResponseList(orders, productMapper);
    }

    public OrderResponse getOrder(Long orderId, Long currentUserId) {
        log.debug("getOrder orderId={}", orderId);
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        
        // Admin (userId=0) can access any order, regular user only their own
        if (currentUserId != 0 && !order.getUser().getId().equals(currentUserId)) {
            throw new OrderAccessDeniedException("Order does not belong to current user");
        }
        
        return orderMapper.toOrderResponse(order, productMapper);
    }

    public List<OrderResponse> getAllOrders() {
        log.debug("getAllOrders (admin)");
        List<Order> orders = orderRepository.findAll();
        return orderMapper.toOrderResponseList(orders, productMapper);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void executeBankPayment(Long orderId, PaymentRequest paymentRequest, Long currentUserId) {
        log.info("executeBankPayment orderId={}", orderId);

        Order order = loadOrderForUser(orderId, currentUserId);

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidPaymentDataException("Order is not in payment status. Current status: " + order.getStatus());
        }

        if (paymentRequest.getCardNumber() == null || paymentRequest.getCvv() == null || paymentRequest.getExpiresAt() == null) {
            throw new InvalidPaymentDataException("Card data is incomplete");
        }

        boolean paymentResult;
        try {
            paymentResult = bankClient.doPayment(paymentRequest, order.getTotalPrice());
        } catch (InvalidPaymentDataException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPaymentDataException("Bank payment error: " + e.getMessage());
        }

        if (!paymentResult) {
            throw new InvalidPaymentDataException("Payment failed");
        }

        order.setStatus(OrderStatus.PAID);
        log.info("order {} marked as PAID", orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markOrderPaid(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markOrderCompleted(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reserveProducts(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        for (OrderItem item : order.getItems()) {
            productReservationService.reserveProduct(item.getProduct().getId(), 1);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseProductReservation(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        for (OrderItem item : order.getItems()) {
            productReservationService.releaseProduct(item.getProduct().getId(), 1);
        }
    }

    public void deductStock(Long orderId) {
        // ProductEntity has no stock field; keep stub for process completeness.
        log.info("Deduct stock stub for order {}", orderId);
    }

    public void sendOrderPaidEmail(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("User with id: " + order.getUser().getId() + " not found"));
        mailSimulator.sendOrderPaidEmail(user.getEmail(), order.getId(), order.getTotalPrice());
    }

    public void sendOrderPaidTelegramNotifications(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("User with id: " + order.getUser().getId() + " not found"));
        publishOrderPaidTelegramNotifications(user, order.getId(), order.getTotalPrice());
    }

    public void publishOrderPaidToBitrix(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        BitrixEisClientJcaAdapter bitrixClient = bitrixEisClientProvider.getIfAvailable();
        if (bitrixClient != null) {
            bitrixClient.publishOrderPaid(order.getId(), order.getUser().getId(), order.getTotalPrice());
        }
    }

    public void validateOrderOwnership(Long orderId, Long userId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessDeniedException("Order does not belong to current user");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new InvalidPaymentDataException("Cannot cancel order. Current status: " + order.getStatus());
        }
    }

    public void validateUserAutoCancellation(Long orderId, Long userId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessDeniedException("Order does not belong to current user");
        }
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidPaymentDataException("User can auto-cancel only CREATED/PENDING_PAYMENT. Current status: " + order.getStatus());
        }
    }

    /**
     * Проверка номера заказа на форме отмены (до отправки администратору).
     */
    public void validateCancellationRequestForm(Long orderId, Long userId) {
        Order order = orderRepository.getOrderById(orderId).orElse(null);
        if (order == null || !order.getUser().getId().equals(userId)) {
            throw new OrderNotFoundException("Заказа с номером " + orderId + " не существует.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidPaymentDataException("Заказ №" + orderId + " уже отменён.");
        }
        if (order.getStatus() == OrderStatus.REFUNDED) {
            throw new InvalidPaymentDataException("Заказ №" + orderId + " уже возвращён и не может быть отменён повторно.");
        }
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidPaymentDataException("Заказ №" + orderId + " уже выполнен и не может быть отменён.");
        }
    }

    /** Заявка пользователя на отмену (финальное решение — у администратора). */
    public Order loadOrderForCancellationRequest(Long orderId, Long userId) {
        validateCancellationRequestForm(orderId, userId);
        return orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
    }

    public void validateOrderCancellable(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED || order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidPaymentDataException("Cannot cancel order. Current status: " + order.getStatus());
        }
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void executeRefund(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        bankClient.refundPayment(order.getTotalPrice());
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void markOrderCancelled(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markOrderRefunded(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
    }

    public void sendOrderCancelledEmail(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("User with id: " + order.getUser().getId() + " not found"));
        mailSimulator.sendOrderCancelledEmail(user.getEmail(), order.getId(), order.getTotalPrice());
    }

    public void executeAdminRefundIfPaid(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.PAID) {
            bankClient.refundPayment(order.getTotalPrice());
        }
    }

    public void markOrderCancelledByAdmin(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidPaymentDataException("Order is already cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatusByAdmin(Long orderId, String targetStatus) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        OrderStatus newStatus = OrderStatus.valueOf(targetStatus.trim().toUpperCase(Locale.ROOT));
        OrderStatus current = order.getStatus();
        boolean allowed = switch (current) {
            case CREATED, PENDING_PAYMENT -> newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.PAID;
            case PAID -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.REFUNDED || newStatus == OrderStatus.CANCELLED;
            case COMPLETED -> newStatus == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!allowed) {
            throw new InvalidPaymentDataException("Illegal status transition: " + current + " -> " + newStatus);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    private Order loadOrderForUser(Long orderId, Long currentUserId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        if (!order.getUser().getId().equals(currentUserId)) {
            throw new OrderAccessDeniedException("Order does not belong to current user");
        }
        return order;
    }

    private void publishOrderPaidTelegramNotifications(User user, Long orderId, java.math.BigDecimal totalPrice) {
        String total = totalPrice.stripTrailingZeros().toPlainString();
        String text = String.format("Заказ #%d оплачен. Сумма: %s ₽.", orderId, total);
        for (UserTelegramLink link : userTelegramLinkRepository.findByUserId(user.getId())) {
            mqttNotificationPublisher.publish(new TelegramNotificationEnvelope(
                    TelegramNotificationEnvelope.TYPE_PLAIN_TEXT,
                    user.getId(),
                    link.getTelegramUsername(),
                    text,
                    null
            ));
        }
    }

}
