package com.mts.online_shop.camunda.listener;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component("cartAddFormValidationTaskListener")
public class CartAddFormValidationTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            return;
        }

        Long productId = longVar(delegateTask, "productId");
        if (productId == null || productId <= 0) {
            String message = "Укажите корректный ID товара";
            delegateTask.setVariable("validationError", message);
            throw new BpmnError("VALIDATION_ERROR", message);
        }
        delegateTask.setVariable("validationError", null);
    }

    private Long longVar(DelegateTask task, String name) {
        Object value = task.getVariable(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        return Long.parseLong(value.toString().trim());
    }
}
