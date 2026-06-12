package com.mts.online_shop.camunda.listener;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component("productUpdateFormValidationTaskListener")
public class ProductUpdateFormValidationTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            return;
        }
        Long productId = longVar(delegateTask, "productId");
        if (productId == null || productId <= 0) {
            throw validationError(delegateTask, "Укажите корректный ID товара");
        }
        String name = stringVar(delegateTask, "name");
        Object priceValue = delegateTask.getVariable("price");
        boolean hasName = name != null && !name.isBlank();
        boolean hasPrice = priceValue != null && !priceValue.toString().isBlank();
        if (!hasName && !hasPrice) {
            throw validationError(delegateTask, "Укажите новое название и/или цену");
        }
        delegateTask.setVariable("validationError", null);
    }

    private static BpmnError validationError(DelegateTask task, String message) {
        task.setVariable("validationError", message);
        return new BpmnError("VALIDATION_ERROR", message);
    }

    private static String stringVar(DelegateTask task, String name) {
        Object value = task.getVariable(name);
        return value == null ? null : value.toString().trim();
    }

    private static Long longVar(DelegateTask task, String name) {
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
