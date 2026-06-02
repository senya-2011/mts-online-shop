package com.mts.onlineshop.controller;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FormService formService;

    @Autowired
    private ProcessEngine processEngine;

    @PostMapping("/start")
    public ResponseEntity<String> startOrderProcess(@RequestParam Long cartId, @RequestParam Long userId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("cartId", cartId);
        variables.put("userId", userId);
        String processInstanceId = runtimeService
                .startProcessInstanceByKey("orderFulfillmentProcess", variables)
                .getId();
        return ResponseEntity.ok("Process started: " + processInstanceId);
    }

    @GetMapping("/tasks/{assignee}")
    public ResponseEntity<List<Map<String, Object>>> getUserTasks(@PathVariable String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
        List<Map<String, Object>> result = tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", task.getId());
            map.put("name", task.getName());
            map.put("processInstanceId", task.getProcessInstanceId());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/task/{taskId}/form")
    public ResponseEntity<?> getTaskForm(@PathVariable String taskId) {
        TaskFormData formData = formService.getTaskFormData(taskId);
        List<FormField> formFields = formData.getFormFields();
        // Возвращаем поля формы клиенту (можно в виде JSON)
        return ResponseEntity.ok(formFields);
    }

    @PostMapping("/task/{taskId}/submit")
    public ResponseEntity<String> submitTaskForm(@PathVariable String taskId, @RequestBody Map<String, Object> formVariables) {
        // Завершаем задачу, передавая значения формы как переменные процесса
        formService.submitTaskForm(taskId, formVariables);
        return ResponseEntity.ok("Task completed, process continues");
    }
}