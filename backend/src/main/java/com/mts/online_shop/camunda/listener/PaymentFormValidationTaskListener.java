package com.mts.online_shop.camunda.listener;

import com.mts.online_shop.camunda.BpmFormVariables;
import com.mts.online_shop.camunda.PaymentCardValidator;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * Нормализует поля формы оплаты. Валидация и {@link org.camunda.bpm.engine.delegate.BpmnError}
 * — в {@link com.mts.online_shop.camunda.delegate.ProcessPaymentDelegate} (boundary на service task).
 */
@Component("paymentFormValidationTaskListener")
public class PaymentFormValidationTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            return;
        }

        String cardNumber = PaymentCardValidator.normalizeCardNumber(
                BpmFormVariables.stringVar(delegateTask, "cardNumber"));
        String cvv = BpmFormVariables.stringVar(delegateTask, "cvv");
        String expiresAt = BpmFormVariables.stringVar(delegateTask, "expiresAt");

        if (cardNumber != null) {
            delegateTask.setVariable("cardNumber", cardNumber);
        }
        if (cvv != null) {
            delegateTask.setVariable("cvv", cvv.trim());
        }
        if (expiresAt != null) {
            delegateTask.setVariable("expiresAt", expiresAt.trim());
        }
    }
}
