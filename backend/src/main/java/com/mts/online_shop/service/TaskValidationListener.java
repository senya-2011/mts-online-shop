package com.mts.online_shop.service;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component("taskValidationListener")
public class TaskValidationListener implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(TaskValidationListener.class);
    private static final Pattern CARD_PATTERN = Pattern.compile("^[0-9]{12,19}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3,4}$");

    @Override
    public void notify(DelegateTask delegateTask) {
        String event = delegateTask.getEventName();
        log.debug("TaskValidationListener event={} task={}", event, delegateTask.getId());
        // Validate on complete event
        if ("complete".equalsIgnoreCase(event)) {
            Object cardObj = delegateTask.getVariable("cardNumber");
            Object cvvObj = delegateTask.getVariable("cvv");
            Object expiresObj = delegateTask.getVariable("expiresAt");

            String card = cardObj == null ? null : cardObj.toString();
            String cvv = cvvObj == null ? null : cvvObj.toString();
            String expires = expiresObj == null ? null : expiresObj.toString();

            if (card == null || !CARD_PATTERN.matcher(card.replaceAll("\\s+","")) .matches()) {
                delegateTask.setVariable("validationMessage", "Неверный номер карты");
                throw new BpmnError("VALIDATION_ERROR", "Invalid card number");
            }
            if (cvv == null || !CVV_PATTERN.matcher(cvv).matches()) {
                delegateTask.setVariable("validationMessage", "Неверный CVV");
                throw new BpmnError("VALIDATION_ERROR", "Invalid CVV");
            }
            if (expires == null || !expires.matches("^(0[1-9]|1[0-2])/(\\d{2})$")) {
                delegateTask.setVariable("validationMessage", "Неверная дата истечения карты (MM/YY)");
                throw new BpmnError("VALIDATION_ERROR", "Invalid expiry");
            }
        }
    }
}
