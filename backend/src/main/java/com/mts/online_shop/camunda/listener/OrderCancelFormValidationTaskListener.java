package com.mts.online_shop.camunda.listener;

import com.mts.online_shop.camunda.BpmFormVariables;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * Нормализует orderId из формы. Проверка существования/статуса — в
 * {@link com.mts.online_shop.camunda.delegate.ValidateCancellationRequestDelegate}.
 */
@Component("orderCancelFormValidationTaskListener")
public class OrderCancelFormValidationTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            return;
        }
        Long orderId = longVar(delegateTask, "orderId");
        if (orderId != null) {
            delegateTask.setVariable("orderId", orderId);
        }
    }

    private Long longVar(DelegateTask task, String name) {
        String raw = BpmFormVariables.stringVar(task, name);
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
