package com.mts.online_shop.camunda.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * Неотмеченный checkbox «Добавить ещё» не передаёт {@code addMore} в процесс — подставляем {@code false}.
 */
@Component("addMoreFormTaskListener")
public class AddMoreFormTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            if (delegateTask.getVariable("addMore") == null) {
                delegateTask.setVariable("addMore", false);
            }
            return;
        }
        if (!EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            return;
        }
        delegateTask.setVariable("addMore", parseBoolean(delegateTask.getVariable("addMore")));
    }

    private static boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return false;
    }
}
