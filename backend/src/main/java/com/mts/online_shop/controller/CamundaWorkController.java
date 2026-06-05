package com.mts.online_shop.controller;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/camunda-work")
public class CamundaWorkController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final FormService formService;

    public CamundaWorkController(RuntimeService runtimeService, TaskService taskService, RepositoryService repositoryService, FormService formService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.formService = formService;
    }

    @GetMapping("/process-definitions")
    public ResponseEntity<List<Map<String, Object>>> listProcessDefinitions() {
        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery().latestVersion().list();
        List<Map<String, Object>> out = defs.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("key", d.getKey());
            m.put("name", d.getName());
            m.put("version", d.getVersion());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startProcess(@RequestParam String key, @RequestBody(required = false) Map<String, Object> variables) {
        if (variables == null) variables = new HashMap<>();
        var pi = runtimeService.startProcessInstanceByKey(key, variables);
        Map<String, Object> resp = Map.of("processInstanceId", pi.getId(), "definitionKey", key);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> listTasks(@RequestParam(required = false) String assignee) {
        List<Task> tasks;
        if (assignee == null || assignee.isBlank()) {
            tasks = taskService.createTaskQuery().list();
        } else {
            tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        }
        List<Map<String, Object>> out = tasks.stream().map(t -> Map.<String, Object>of(
            "id", t.getId(),
            "name", t.getName(),
            "assignee", t.getAssignee(),
            "processInstanceId", t.getProcessInstanceId()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @PostMapping("/task/{id}/claim")
    public ResponseEntity<String> claimTask(@PathVariable String id, @RequestParam String user) {
        taskService.claim(id, user);
        return ResponseEntity.ok("claimed");
    }

    @PostMapping("/task/{id}/complete")
    public ResponseEntity<String> completeTask(@PathVariable String id, @RequestBody(required = false) Map<String, Object> variables) {
        if (variables == null) variables = new HashMap<>();
        taskService.complete(id, variables);
        return ResponseEntity.ok("completed");
    }

    @GetMapping("/task/{id}/form-variables")
    public ResponseEntity<Map<String, Object>> getFormVariables(@PathVariable String id) {
        Map<String, Object> vars = formService.getTaskFormData(id).getFormFields().stream()
            .collect(Collectors.toMap(f -> f.getId(), f -> (Object) Map.of("label", f.getLabel(), "type", f.getType().getName())));
        return ResponseEntity.ok(vars);
    }
}
