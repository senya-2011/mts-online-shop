package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmOrderCheckoutService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("reserveProductsDelegate")
public class ReserveProductsDelegate implements JavaDelegate {

    private final BpmOrderCheckoutService bpmOrderCheckoutService;

    public ReserveProductsDelegate(BpmOrderCheckoutService bpmOrderCheckoutService) {
        this.bpmOrderCheckoutService = bpmOrderCheckoutService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        if (Boolean.TRUE.equals(execution.getVariable("productsReserved"))) {
            return;
        }
        Long orderId = DelegateVariables.requiredLong(execution, "orderId", "Order id is required.");
        bpmOrderCheckoutService.reserveOrderProducts(orderId);
        execution.setVariable("productsReserved", true);
    }
}
