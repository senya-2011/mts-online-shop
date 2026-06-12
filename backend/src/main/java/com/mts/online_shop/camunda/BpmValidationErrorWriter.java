package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

/**
 * Сохраняет {@code validationError} вне транзакции Camunda — иначе при {@link org.camunda.bpm.engine.delegate.BpmnError}
 * переменная откатывается и пользователь не видит текст в Tasklist.
 */
@Component
public class BpmValidationErrorWriter {

    private final RuntimeService runtimeService;
    private final BpmDelegateSupport bpmDelegateSupport;

    public BpmValidationErrorWriter(RuntimeService runtimeService, BpmDelegateSupport bpmDelegateSupport) {
        this.runtimeService = runtimeService;
        this.bpmDelegateSupport = bpmDelegateSupport;
    }

    public void setValidationError(String processInstanceId, String message) {
        bpmDelegateSupport.runInNewTransaction(() -> {
            if (message == null || message.isBlank()) {
                runtimeService.removeVariable(processInstanceId, "validationError");
            } else {
                runtimeService.setVariable(processInstanceId, "validationError", message);
            }
        });
    }
}
