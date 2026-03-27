package com.mts.online_shop.service;

import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.model.Order;
import com.mts.online_shop.model.PaymentRequest;
import com.mts.online_shop.repository.OrderRepository;
import com.mts.online_shop.client.bank.BankClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.transaction.Transactional;

@Service
public class DistributedTransactionService {

    private static final Logger log = LoggerFactory.getLogger(DistributedTransactionService.class);
    private final OrderRepository orderRepository;
    private final BankClient bankClient;

    public DistributedTransactionService(OrderRepository orderRepository, BankClient bankClient) {
        this.orderRepository = orderRepository;
        this.bankClient = bankClient;
    }

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void executePaymentTransaction(Long orderId, PaymentRequest paymentRequest) {
        log.info("Executing distributed payment transaction for orderId={}", orderId);
        
        try {
            Order order = orderRepository.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            boolean paymentResult = bankClient.doPayment(paymentRequest, order.getTotalPrice());
            
            if (!paymentResult) {
                throw new InvalidPaymentDataException("Payment failed for order: " + orderId);
            }
            
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            
            log.info("Distributed transaction completed successfully for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Distributed transaction failed for orderId={}: {}", orderId, e.getMessage());
            throw e;
        }
    }
}
