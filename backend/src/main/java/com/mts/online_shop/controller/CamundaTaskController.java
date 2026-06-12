package com.mts.online_shop.controller;

import com.mts.online_shop.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/camunda/tasks")
@SecurityRequirement(name = "jwtAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Camunda Tasks", description = "Задачи BPM-процессов (формы Camunda)")
public class CamundaTaskController {

    private final TaskService taskService;
    private final FormService formService;
    private final CurrentUserService currentUserService;

    public CamundaTaskController(
            TaskService taskService,
            FormService formService,
            CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.formService = formService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listMyTasks() {
        String camundaUserId = currentUserService.getCurrentUserLogin().toLowerCase();
        List<Task> tasks = accessibleTaskQuery(camundaUserId).active().list();
        List<Map<String, Object>> result = tasks.stream().map(task -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("name", task.getName());
            item.put("processInstanceId", task.getProcessInstanceId());
            item.put("taskDefinitionKey", task.getTaskDefinitionKey());
            item.put("formKey", task.getFormKey());
            item.put("assignee", task.getAssignee());
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{taskId}/form")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getTaskForm(@PathVariable String taskId) {
        assertTaskAccessible(taskId);
        TaskFormData formData = formService.getTaskFormData(taskId);
        Map<String, Object> response = new HashMap<>();
        response.put("formKey", formData.getFormKey());
        response.put("deploymentId", formData.getDeploymentId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{taskId}/complete")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> completeTask(@PathVariable String taskId, @RequestBody Map<String, Object> variables) {
        assertTaskAccessible(taskId);
        taskService.complete(taskId, variables);
        return ResponseEntity.ok().build();
    }

    private void assertTaskAccessible(String taskId) {
        String camundaUserId = currentUserService.getCurrentUserLogin().toLowerCase();
        long count = accessibleTaskQuery(camundaUserId)
                .taskId(taskId)
                .count();
        if (count == 0) {
            throw new org.springframework.security.access.AccessDeniedException("Task not accessible");
        }
    }

    private TaskQuery accessibleTaskQuery(String camundaUserId) {
        return taskService.createTaskQuery()
                .or()
                .taskAssignee(camundaUserId)
                .taskCandidateUser(camundaUserId)
                .endOr();
    }
}
