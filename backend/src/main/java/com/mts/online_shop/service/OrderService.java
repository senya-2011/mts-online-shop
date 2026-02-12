package com.mts.online_shop.service;

import com.mts.online_shop.exception.EmptyCartException;
import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.model.*;
import com.mts.online_shop.repository.OrderRepository;
import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.simulator.bank.BankSimulator;
import com.mts.online_shop.simulator.mail.MailSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final GoodsService goodsService;
    private final UserRepository userRepository;
    private final BankSimulator bankSimulator;
    private final MailSimulator mailSimulator;

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper,
                        GoodsService goodsService,
                        UserRepository userRepository,
                        BankSimulator bankSimulator,
                        MailSimulator mailSimulator) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.goodsService = goodsService;
        this.userRepository = userRepository;
        this.bankSimulator = bankSimulator;
        this.mailSimulator = mailSimulator;
    }

    public OrderResponse getOrderByOrderId(Long orderId) {
        log.debug("getOrderByOrderId orderId={}", orderId);
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Long userId = orderRequest.getUserId();
        log.info("createOrder userId={}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        List<Product> productsInCart = goodsService.findUserGoods(userId);
        if (productsInCart.isEmpty()) {
            throw new EmptyCartException("Cart for user with id: " + userId + " is empty");
        }

        Order order = orderMapper.toOrder(orderRequest);

        List<OrderItem> orderItems = orderMapper.toOrderItems(order, productsInCart);

        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        goodsService.clearCart(userId);
        log.info("order created id={}", savedOrder.getId());
        return orderMapper.toOrderResponse(savedOrder);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.debug("getOrdersByUserId userId={}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));
        List<Order> orders = orderRepository.getOrdersByUserId(userId);
        return orderMapper.toOrderResponseList(orders);
    }

    @Transactional
    public OrderResponse payOrder(Long orderId, PaymentRequest paymentRequest) {
        log.info("payOrder orderId={}", orderId);
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        
        boolean paymentResult = bankSimulator.doPayment(paymentRequest, order.getTotalPrice());
        if (!paymentResult) {
            log.warn("payment failed orderId={}", orderId);
            throw new InvalidPaymentDataException("Payment failed");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        log.info("order paid orderId={}", orderId);

        if (order.getUser() != null) {
            mailSimulator.sendOrderPaidEmail(order.getUser().getEmail(), order.getId(), order.getTotalPrice());
        }

        return orderMapper.toOrderResponse(order);
    }
}
