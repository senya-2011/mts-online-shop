package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmDelegateSupport;
import com.mts.online_shop.camunda.BpmFormVariables;
import com.mts.online_shop.camunda.BpmUserIdResolver;
import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.model.Order;
import com.mts.online_shop.service.OrderService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("validateCancellationRequestDelegate")
public class ValidateCancellationRequestDelegate implements JavaDelegate {

    private static final String VALIDATION_OK = "OK";

    private final OrderService orderService;
    private final BpmUserIdResolver bpmUserIdResolver;
    private final BpmDelegateSupport bpmDelegateSupport;

    public ValidateCancellationRequestDelegate(
            OrderService orderService,
            BpmUserIdResolver bpmUserIdResolver,
            BpmDelegateSupport bpmDelegateSupport) {
        this.orderService = orderService;
        this.bpmUserIdResolver = bpmUserIdResolver;
        this.bpmDelegateSupport = bpmDelegateSupport;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = parseOrderId(execution);
        if (orderId == null || orderId <= 0) {
            reject(execution, "Укажите корректный номер заказа.");
            return;
        }

        Long userId;
        try {
            userId = bpmUserIdResolver.requiredUserId(execution);
        } catch (BpmnError ex) {
            reject(execution, ex.getMessage());
            return;
        }

        try {
            Order order = bpmDelegateSupport.callInNewTransaction(
                    () -> orderService.loadOrderForCancellationRequest(orderId, userId));
            execution.setVariable("orderId", order.getId());
            execution.setVariable("customerLogin", order.getUser().getLogin());
            execution.setVariable("validationResult", VALIDATION_OK);
            execution.setVariable("validationError", null);
        } catch (OrderNotFoundException ex) {
            reject(execution, ex.getMessage());
        } catch (InvalidPaymentDataException ex) {
            reject(execution, ex.getMessage());
        }
    }

    private static void reject(DelegateExecution execution, String message) {
        execution.setVariable("validationResult", "ERROR");
        execution.setVariable("validationError", message);
    }

    private static Long parseOrderId(DelegateExecution execution) {
        String raw = BpmFormVariables.stringVar(execution, "orderId");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
