package com.mts.online_shop.camunda.delegate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;

final class DelegateVariables {

    private DelegateVariables() {
    }

    static Long requiredLong(DelegateExecution execution, String name, String hint) {
        Object value = execution.getVariable(name);
        if (value == null) {
            throw new BpmnError(
                    "MISSING_VARIABLE",
                    "Не задана переменная '" + name + "'. " + hint);
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        if (value instanceof Double doubleValue) {
            return doubleValue.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue.trim());
            } catch (NumberFormatException e) {
                throw new BpmnError(
                        "INVALID_VARIABLE",
                        "Переменная '" + name + "' должна быть числом, получено: " + stringValue);
            }
        }
        throw new BpmnError(
                "INVALID_VARIABLE",
                "Переменная '" + name + "' должна быть числом, получено: " + value.getClass().getSimpleName());
    }
}
