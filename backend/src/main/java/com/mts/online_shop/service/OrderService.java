package com.mts.online_shop.service;

import com.mts.online_shop.exception.EmptyCartException;
import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.exception.OrderAccessDeniedException;
import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.mapper.ProductMapper;
import com.mts.online_shop.model.*;
import com.mts.online_shop.repository.OrderRepository;
import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.client.bank.BankClient;
import com.mts.online_shop.simulator.mail.MailSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.transaction.Transactional;

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

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper,
                        ProductMapper productMapper,
                        GoodsService goodsService,
                        UserRepository userRepository,
                        BankClient bankClient,
                        MailSimulator mailSimulator) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.goodsService = goodsService;
        this.userRepository = userRepository;
        this.bankClient = bankClient;
        this.mailSimulator = mailSimulator;
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

    public List<com.mts.online_shop.model.OrderResponse> getOrdersByUserId(Long userId) {
        log.debug("getOrdersByUserId userId={}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        List<Order> orders = orderRepository.getOrdersByUserId(userId);
        return orderMapper.toOrderResponseList(orders, productMapper);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = {InvalidPaymentDataException.class, OrderNotFoundException.class, 
                              OrderAccessDeniedException.class, UserNotFoundException.class, RuntimeException.class})
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

        boolean paymentResult = bankClient.doPayment(paymentRequest, order.getTotalPrice());
        if (!paymentResult) {
            log.warn("payment failed orderId={}", orderId);
            throw new InvalidPaymentDataException("Payment failed");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        
        log.info("order paid orderId={} for user={}", orderId, user.getId());

        mailSimulator.sendOrderPaidEmail(user.getEmail(), order.getId(), order.getTotalPrice());
    }
}
