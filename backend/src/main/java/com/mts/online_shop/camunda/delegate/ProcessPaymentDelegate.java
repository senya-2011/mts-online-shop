package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmFormVariables;
import com.mts.online_shop.camunda.PaymentCardValidator;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Lab-оплата: только regex-валидация карты, без вызова банка.
 * При успехе {@code paymentSuccess=true} и процесс идёт дальше.
 */
@Component("processPaymentDelegate")
public class ProcessPaymentDelegate implements JavaDelegate {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    @Override
    public void execute(DelegateExecution execution) {
        String cardNumber = BpmFormVariables.stringVar(execution, "cardNumber");
        String cvv = BpmFormVariables.stringVar(execution, "cvv");
        String expiresAt = BpmFormVariables.stringVar(execution, "expiresAt");

        String validationError = PaymentCardValidator.validate(cardNumber, cvv, expiresAt);
        if (validationError != null) {
            execution.setVariable("validationError", validationError);
            execution.setVariable("paymentSuccess", false);
            execution.setVariable("paymentError", validationError);
            throw new BpmnError(VALIDATION_ERROR, validationError);
        }

        execution.setVariable("cardNumber", PaymentCardValidator.normalizeCardNumber(cardNumber));
        execution.setVariable("cvv", cvv.trim());
        execution.setVariable("expiresAt", expiresAt.trim());
        execution.setVariable("paymentSuccess", true);
        execution.setVariable("paymentError", null);
        execution.setVariable("validationError", null);
    }
}
