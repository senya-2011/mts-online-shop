package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.service.OrderService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("sendTelegramNotificationDelegate")
public class SendTelegramNotificationDelegate implements JavaDelegate {

    private final OrderService orderService;

    public SendTelegramNotificationDelegate(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        orderService.sendOrderPaidTelegramNotifications(orderId);
    }
}
