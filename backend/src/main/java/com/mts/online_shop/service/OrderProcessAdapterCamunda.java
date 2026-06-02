package com.mts.online_shop.service;

import com.mts.online_shop.model.PaymentRequest;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("orderProcessAdapter")
public class OrderProcessAdapterCamunda implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessAdapterCamunda.class);

    @Autowired
    private OrderService orderService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        String activityId = execution.getCurrentActivityId();
        log.debug("OrderProcessAdapterCamunda execute activityId={}", activityId);

        switch (activityId) {
            case "CreateOrderTask":
                handleCreateOrder(execution);
                break;
            case "ConfirmPaymentTask":
                handleConfirmPayment(execution);
                break;
            case "CancelOrderTask":
                handleCancelOrder(execution);
                break;
            case "CleanupTask":
                handleCleanup(execution);
                break;
            default:
                log.warn("Unknown activity for OrderProcessAdapterCamunda: {}", activityId);
        }
    }

    private void handleCreateOrder(DelegateExecution execution) {
        Long userId = (Long) execution.getVariable("userId");
        Long orderId = orderService.createOrder(userId);
        execution.setVariable("orderId", orderId);
        execution.setVariable("paymentReceived", false);
    }

    private void handleConfirmPayment(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Long userId = (Long) execution.getVariable("userId");

        String cardNumber = (String) execution.getVariable("cardNumber");
        String cvv = (String) execution.getVariable("cvv");
        String expiresAt = (String) execution.getVariable("expiresAt");

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setCardNumber(cardNumber);
        paymentRequest.setCvv(cvv);
        paymentRequest.setExpiresAt(expiresAt);

        orderService.payOrder(orderId, paymentRequest, userId);
        execution.setVariable("paymentReceived", true);
    }

    private void handleCancelOrder(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Long userId = (Long) execution.getVariable("userId");
        Boolean adminCancel = (Boolean) execution.getVariable("adminCancel");
        if (adminCancel != null && adminCancel.booleanValue()) {
            orderService.adminCancelOrder(orderId);
        } else {
            orderService.cancelOrder(orderId, userId);
        }
        execution.setVariable("paymentReceived", false);
    }

    private void handleCleanup(DelegateExecution execution) {
        Long olderThanMs = (Long) execution.getVariable("olderThanMs");
        if (olderThanMs == null) {
            // default to 24 hours
            olderThanMs = 24L * 60L * 60L * 1000L;
        }
        int cleaned = orderService.cleanupExpiredOrders(olderThanMs);
        log.info("CleanupTask removed {} expired orders", cleaned);
    }
}
