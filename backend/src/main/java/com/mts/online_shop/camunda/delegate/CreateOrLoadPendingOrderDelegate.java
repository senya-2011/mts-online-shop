package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmOrderCheckoutService;
import com.mts.online_shop.camunda.BpmUserIdResolver;
import com.mts.online_shop.service.OrderService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("createOrLoadPendingOrderDelegate")
public class CreateOrLoadPendingOrderDelegate implements JavaDelegate {

    private final OrderService orderService;
    private final BpmOrderCheckoutService bpmOrderCheckoutService;
    private final BpmUserIdResolver bpmUserIdResolver;

    public CreateOrLoadPendingOrderDelegate(
            OrderService orderService,
            BpmOrderCheckoutService bpmOrderCheckoutService,
            BpmUserIdResolver bpmUserIdResolver) {
        this.orderService = orderService;
        this.bpmOrderCheckoutService = bpmOrderCheckoutService;
        this.bpmUserIdResolver = bpmUserIdResolver;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long userId = bpmUserIdResolver.requiredUserId(execution);
        Long orderId = longVar(execution, "orderId");
        if (orderId == null) {
            orderId = bpmOrderCheckoutService.createOrderFromCartAndReserve(userId);
            execution.setVariable("productsReserved", true);
        } else {
            orderService.ensureOrderPendingPayment(orderId, userId);
        }
        execution.setVariable("orderId", orderId);
    }

    private Long longVar(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        return null;
    }
}
