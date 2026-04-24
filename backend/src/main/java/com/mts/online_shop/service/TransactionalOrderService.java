package com.mts.online_shop.service;

import com.mts.online_shop.exception.EmptyCartException;
import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.model.*;
import com.mts.online_shop.repository.OrderRepository;
import com.mts.online_shop.repository.UserItemRepository;
import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.client.bank.BankClient;
import com.mts.online_shop.client.bank.BankClientProperties;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.mapper.ProductMapper;
import com.mts.online_shop.simulator.mail.MailSimulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionalOrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserItemRepository userItemRepository;
    private final BankClient bankClient;
    private final BankClientProperties bankClientProperties;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final MailSimulator mailSimulator;

    public TransactionalOrderService(UserRepository userRepository, OrderRepository orderRepository,
                                    UserItemRepository userItemRepository, BankClient bankClient,
                                    BankClientProperties bankClientProperties, OrderMapper orderMapper,
                                    ProductMapper productMapper, MailSimulator mailSimulator) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.userItemRepository = userItemRepository;
        this.bankClient = bankClient;
        this.bankClientProperties = bankClientProperties;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.mailSimulator = mailSimulator;
    }

    @Transactional(rollbackFor = {EmptyCartException.class, InvalidPaymentDataException.class})
    public Order createOrderWithPayment(Long userId, PaymentRequest paymentRequest) throws EmptyCartException, InvalidPaymentDataException, UserNotFoundException {
        // 1. Проверяем существование пользователя
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }
        User user = userOpt.get();

        // 2. Получаем товары из корзины
        List<UserItem> items = userItemRepository.findByUser_Id(userId);
        if (items.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        // 3. Рассчитываем общую стоимость
        BigDecimal totalPrice = items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Создаем заказ
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(totalPrice);
        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());
        order.setOrderNumber(UUID.randomUUID().toString());

        // 5. Сохраняем заказ
        Order savedOrder = orderRepository.save(order);

        // 6. Обрабатываем платеж
        boolean paymentSuccessful = bankClient.doPayment(paymentRequest, totalPrice);
        if (!paymentSuccessful) {
            throw new InvalidPaymentDataException("Payment failed");
        }

        // 7. Обновляем статус заказа
        savedOrder.setStatus(OrderStatus.PAID);
        savedOrder.setUpdatedAt(new Date());
        orderRepository.save(savedOrder);

        // 8. Очищаем корзину
        userItemRepository.deleteAllByUser_Id(userId);

        // 9. Отправляем email уведомление
        mailSimulator.sendOrderConfirmation(user.getEmail(), savedOrder.getOrderNumber());

        return savedOrder;
    }

    @Transactional(rollbackFor = OrderNotFoundException.class)
    public void cancelOrderWithRefund(Long orderId) throws OrderNotFoundException {
        // 1. Находим заказ
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new OrderNotFoundException("Order not found");
        }
        Order order = orderOpt.get();

        // 2. Проверяем статус заказа
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        }

        // 3. Обновляем статус заказа
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(new Date());
        orderRepository.save(order);

        // 4. Возвращаем товары в корзину 
        // Для упрощения просто отправляем уведомление
        mailSimulator.sendOrderCancellation(order.getUser().getEmail(), order.getOrderNumber());
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }
}
