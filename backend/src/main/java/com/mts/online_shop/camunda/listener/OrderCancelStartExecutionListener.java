package com.mts.online_shop.camunda.listener;

import com.mts.online_shop.camunda.BpmUserIdResolver;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Старт {@code user-order-cancel}: {@code userId} из таблицы users по инициатору.
 */
@Component("orderCancelStartExecutionListener")
public class OrderCancelStartExecutionListener implements ExecutionListener {

    private final BpmUserIdResolver bpmUserIdResolver;

    public OrderCancelStartExecutionListener(BpmUserIdResolver bpmUserIdResolver) {
        this.bpmUserIdResolver = bpmUserIdResolver;
    }

    @Override
    public void notify(DelegateExecution execution) {
        bpmUserIdResolver.reconcileExecutionUser(execution);
    }
}
