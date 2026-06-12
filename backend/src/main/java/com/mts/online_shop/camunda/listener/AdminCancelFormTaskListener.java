package com.mts.online_shop.camunda.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * Форма подтверждения отмены админом: неотмеченный checkbox → отклонение заявки.
 */
@Component("adminCancelFormTaskListener")
public class AdminCancelFormTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            if (delegateTask.getVariable("cancelApproved") == null) {
                delegateTask.setVariable("cancelApproved", false);
            }
            return;
        }
        if (!EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            return;
        }
        boolean approved = parseBoolean(delegateTask.getVariable("cancelApproved"));
        delegateTask.setVariable("cancelApproved", approved);
        if (!approved) {
            delegateTask.setVariable("cancelRejectionReason", "Администратор отклонил отмену заказа");
        } else {
            delegateTask.setVariable("cancelRejectionReason", null);
        }
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
