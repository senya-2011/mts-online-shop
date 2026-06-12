package com.mts.online_shop.camunda.listener;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component("productCreateFormValidationTaskListener")
public class ProductCreateFormValidationTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            return;
        }
        String name = stringVar(delegateTask, "name");
        Object priceValue = delegateTask.getVariable("price");
        if (name == null || name.isBlank()) {
            throw validationError(delegateTask, "Укажите название товара");
        }
        if (priceValue == null) {
            throw validationError(delegateTask, "Укажите цену товара");
        }
        double price = priceValue instanceof Number number ? number.doubleValue() : Double.parseDouble(priceValue.toString());
        if (price < 0) {
            throw validationError(delegateTask, "Цена не может быть отрицательной");
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
}
