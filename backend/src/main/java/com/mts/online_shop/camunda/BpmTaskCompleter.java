package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BpmTaskCompleter {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public BpmTaskCompleter(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    public ProcessInstance startAndCompleteUserTask(
            String processKey,
            Map<String, Object> startVariables,
            String userTaskDefinitionKey,
            Map<String, Object> formVariables) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, startVariables);
        completeUserTask(processInstance.getId(), userTaskDefinitionKey, formVariables);
        return processInstance;
    }

    public void completeUserTask(String processInstanceId, String userTaskDefinitionKey, Map<String, Object> formVariables) {
        Task task = findActiveUserTask(processInstanceId, userTaskDefinitionKey);
        if (task == null) {
            throw new IllegalStateException("User task not found: " + userTaskDefinitionKey + " in " + processInstanceId);
        }
        taskService.complete(task.getId(), formVariables);
    }

    public Task findActiveUserTask(String processInstanceId, String userTaskDefinitionKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(userTaskDefinitionKey)
                .singleResult();
    }
}
