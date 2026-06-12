package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.service.OrderService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("sendOrderCancelledEmailDelegate")
public class SendOrderCancelledEmailDelegate implements JavaDelegate {

    private final OrderService orderService;

    public SendOrderCancelledEmailDelegate(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        orderService.sendOrderCancelledEmail(orderId);
    }
}
