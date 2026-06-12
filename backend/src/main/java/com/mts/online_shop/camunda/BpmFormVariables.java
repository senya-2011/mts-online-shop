package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;

import java.math.BigDecimal;

/**
 * Camunda Tasklist forms may submit numbers as {@link Double} — normalize to plain strings.
 */
public final class BpmFormVariables {

    private BpmFormVariables() {
    }

    public static String stringVar(DelegateTask task, String name) {
        Object value = task.getVariableLocal(name);
        if (value == null) {
            value = task.getVariable(name);
        }
        return toPlainString(value);
    }

    public static String stringVar(DelegateExecution execution, String name) {
        return toPlainString(execution.getVariable(name));
    }

    private static String toPlainString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue.trim();
        }
        if (value instanceof Long longValue) {
            return Long.toString(longValue);
        }
        if (value instanceof Integer intValue) {
            return Integer.toString(intValue);
        }
        if (value instanceof BigDecimal decimalValue) {
            return decimalValue.toPlainString();
        }
        if (value instanceof Double || value instanceof Float) {
            return BigDecimal.valueOf(((Number) value).doubleValue()).toPlainString();
        }
        return value.toString().trim();
    }
}
