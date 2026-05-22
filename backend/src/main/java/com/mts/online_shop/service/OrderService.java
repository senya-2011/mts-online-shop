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

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final GoodsService goodsService;
    private final UserRepository userRepository;
    private final BankClient bankClient;
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

    public void payOrder(Long orderId, com.mts.online_shop.model.PaymentRequest paymentRequest, Long currentUserId) {
        log.info("payOrder orderId={}", orderId);
        
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        
        if (!order.getUser().getId().equals(currentUserId)) {
            throw new OrderAccessDeniedException("Order does not belong to current user");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidPaymentDataException("Order is not in payment status. Current status: " + order.getStatus());
        }

        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("User with id: " + order.getUser().getId() + " not found"));

        // РЕАЛЬНЫЙ ВЫЗОВ БАНКА из директории bank
        log.info("Calling real bank for payment orderId={}", orderId);
        log.info("Payment data: card={}, cvv={}, expiresAt={}", 
            paymentRequest.getCardNumber() != null ? "****" + paymentRequest.getCardNumber().substring(Math.max(0, paymentRequest.getCardNumber().length() - 4)) : "null",
            paymentRequest.getCvv() != null ? "***" : "null",
            paymentRequest.getExpiresAt());
        
        if (paymentRequest.getCardNumber() == null || paymentRequest.getCvv() == null || paymentRequest.getExpiresAt() == null) {
            log.error("Payment data is incomplete");
            throw new InvalidPaymentDataException("Card data is incomplete");
        }
        
        boolean paymentResult;
        try {
            paymentResult = bankClient.doPayment(paymentRequest, order.getTotalPrice());
        } catch (InvalidPaymentDataException e) {
            log.error("Bank payment failed with message: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Bank payment failed with exception: {} - {}", e.getClass().getName(), e.getMessage());
            throw new InvalidPaymentDataException("Bank payment error: " + e.getMessage());
        }
        
        if (!paymentResult) {
            log.warn("payment failed orderId={}", orderId);
            throw new InvalidPaymentDataException("Payment failed");
        }

        // Устанавливаем статус PAID (не сохраняем в БД из-за проблемы с Hibernate коллекцией)
        order.setStatus(OrderStatus.PAID);
        log.info("order {} marked as PAID (status not saved to DB due to Hibernate issue)", orderId);

        mailSimulator.sendOrderPaidEmail(user.getEmail(), order.getId(), order.getTotalPrice());
        publishOrderPaidTelegramNotifications(user, order.getId(), order.getTotalPrice());
        BitrixEisClientJcaAdapter bitrixClient = bitrixEisClientProvider.getIfAvailable();
        if (bitrixClient != null) {
            bitrixClient.publishOrderPaid(order.getId(), user.getId(), order.getTotalPrice());
        }
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

    @Transactional(rollbackFor = {EmptyCartException.class, UserNotFoundException.class, RuntimeException.class})
    public OrderResponse createOrderWithPayment(Long userId, PaymentRequest paymentRequest) {
        log.info("createOrderWithPayment userId={}", userId);
        
        // Создаем заказ
        Long orderId = createOrder(userId);
        
        // Получаем созданный заказ
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        
        // Оплачиваем заказ с данными карты от пользователя
        payOrder(orderId, paymentRequest, userId);
        
        // Возвращаем обновленный заказ
        Order updatedOrder = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        
        return orderMapper.toOrderResponse(updatedOrder, productMapper);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        log.info("cancelOrder orderId={} userId={}", orderId, userId);
        
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        
        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessDeniedException("Order does not belong to current user");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidPaymentDataException("Cannot cancel order. Current status: " + order.getStatus());
        }

        // Возврат денег
        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("User with id: " + order.getUser().getId() + " not found"));
        
        bankClient.refundPayment(order.getTotalPrice());
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        log.info("order cancelled orderId={} for user={}", orderId, user.getId());
        mailSimulator.sendOrderCancelledEmail(user.getEmail(), order.getId(), order.getTotalPrice());
    }

    @Transactional
    public void adminCancelOrder(Long orderId) {
        log.info("adminCancelOrder orderId={}", orderId);
        
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        
        // Админ может отменить заказ с любым статусом
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidPaymentDataException("Order is already cancelled");
        }
        
        // Возврат денег только если заказ был оплачен
        if (order.getStatus() == OrderStatus.PAID) {
            User user = userRepository.findById(order.getUser().getId())
                    .orElseThrow(() -> new UserNotFoundException("User with id: " + order.getUser().getId() + " not found"));
            bankClient.refundPayment(order.getTotalPrice());
            mailSimulator.sendOrderCancelledEmail(user.getEmail(), order.getId(), order.getTotalPrice());
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        log.info("order cancelled by admin orderId={}", orderId);
    }
}
