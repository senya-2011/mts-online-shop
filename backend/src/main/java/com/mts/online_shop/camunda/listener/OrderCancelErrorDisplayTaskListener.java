package com.mts.online_shop.camunda.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * Подставляет {@code validationError} в форму и описание задачи Tasklist.
 */
@Component("orderCancelErrorDisplayTaskListener")
public class OrderCancelErrorDisplayTaskListener implements TaskListener {

    private static final String DEFAULT_TASK_NAME = "Номер заказа для отмены";

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            return;
        }
        Object error = delegateTask.getVariable("validationError");
        if (error == null || error.toString().isBlank()) {
            delegateTask.setName(DEFAULT_TASK_NAME);
            return;
        }
        String message = error.toString();
        delegateTask.setVariableLocal("validationError", message);
        delegateTask.setDescription(message);
        delegateTask.setName(DEFAULT_TASK_NAME + " — " + message);
    }
}
