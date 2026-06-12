package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.service.OrderService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("markOrderCompletedDelegate")
public class MarkOrderCompletedDelegate implements JavaDelegate {

    private final OrderService orderService;

    public MarkOrderCompletedDelegate(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = DelegateVariables.requiredLong(execution, "orderId", "Order id is required.");
        orderService.markOrderCompleted(orderId);
    }
}
