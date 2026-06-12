package com.mts.online_shop.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "BPM-процесс оформления заказа запущен, оплата — в Camunda Tasklist")
public class OrderBpmStartResponse {

    private String processInstanceId;
    private String taskId;
    private String taskName;
    private String message;
    private String accessToken;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
