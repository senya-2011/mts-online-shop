package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmOrderCheckoutService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("markOrderPaidDelegate")
public class MarkOrderPaidDelegate implements JavaDelegate {

    private final BpmOrderCheckoutService bpmOrderCheckoutService;

    public MarkOrderPaidDelegate(BpmOrderCheckoutService bpmOrderCheckoutService) {
        this.bpmOrderCheckoutService = bpmOrderCheckoutService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = DelegateVariables.requiredLong(execution, "orderId", "Order id is required.");
        bpmOrderCheckoutService.markOrderPaid(orderId);
    }
}
