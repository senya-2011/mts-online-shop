package com.mts.online_shop.camunda.listener;

import com.mts.online_shop.camunda.BpmUserIdResolver;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * При старте {@code user-lk-order} выставляет {@code userId} из таблицы users
 * по логину инициатора (Tasklist или API).
 */
@Component("lkOrderStartExecutionListener")
public class LkOrderStartExecutionListener implements ExecutionListener {

    private final BpmUserIdResolver bpmUserIdResolver;

    public LkOrderStartExecutionListener(BpmUserIdResolver bpmUserIdResolver) {
        this.bpmUserIdResolver = bpmUserIdResolver;
    }

    @Override
    public void notify(DelegateExecution execution) {
        bpmUserIdResolver.reconcileExecutionUser(execution);
    }
}
