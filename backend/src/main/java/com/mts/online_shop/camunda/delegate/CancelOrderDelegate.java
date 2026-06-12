package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmOrderCheckoutService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("cancelOrderDelegate")
public class CancelOrderDelegate implements JavaDelegate {

    private final BpmOrderCheckoutService bpmOrderCheckoutService;

    public CancelOrderDelegate(BpmOrderCheckoutService bpmOrderCheckoutService) {
        this.bpmOrderCheckoutService = bpmOrderCheckoutService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = DelegateVariables.requiredLong(execution, "orderId", "Order id is required.");
        bpmOrderCheckoutService.cancelOrderApproved(orderId);
    }
}
