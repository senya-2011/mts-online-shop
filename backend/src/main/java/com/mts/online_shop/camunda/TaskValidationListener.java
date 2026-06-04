package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class TaskValidationListener implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(TaskValidationListener.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        String taskKey = delegateTask.getTaskDefinitionKey();
        log.debug("Running validation for task: {} on execution {}", taskKey, delegateTask.getExecutionId());

        try {
            if ("Activity_0jl0p2s".equals(taskKey)) { // card input task
                Object cardNumber = delegateTask.getVariable("cardNumber");
                Object expiry = delegateTask.getVariable("cardExpiry");
                Object cvv = delegateTask.getVariable("cardCvv");

                StringBuilder err = new StringBuilder();
                if (cardNumber == null || Objects.toString(cardNumber).trim().length() < 12) {
                    err.append("Invalid card number. ");
                }
                if (expiry == null || Objects.toString(expiry).trim().length() < 3) {
                    err.append("Invalid expiry. ");
                }
                if (cvv == null || Objects.toString(cvv).trim().length() < 3) {
                    err.append("Invalid CVV. ");
                }

                if (err.length() > 0) {
                    String message = err.toString().trim();
                    log.info("Validation failed for task {}: {}", taskKey, message);
                    delegateTask.setVariable("validationErrorMessage", message);
                    throw new BpmnError("VALIDATION_ERROR", message);
                }
            }
            // add further task-specific validations here
        } catch (BpmnError be) {
            throw be; // rethrow to be handled by boundary event
        } catch (Exception e) {
            log.error("Unexpected validation error", e);
            delegateTask.setVariable("validationErrorMessage", "Internal validation error");
            throw new BpmnError("VALIDATION_ERROR", "Internal validation error");
        }
    }
}
