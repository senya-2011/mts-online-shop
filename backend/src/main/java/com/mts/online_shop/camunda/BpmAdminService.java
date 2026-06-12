package com.mts.online_shop.camunda;

import com.mts.online_shop.model.OrderBpmStartResponse;
import com.mts.online_shop.security.CurrentUserService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BpmAdminService {

    public static final String PROCESS_ADMIN_PRODUCT_CREATE = "admin-product-create";
    public static final String PROCESS_ADMIN_PRODUCT_UPDATE = "admin-product-update";
    public static final String TASK_CREATE_PRODUCT = "Task_CreateProductForm";
    public static final String TASK_UPDATE_PRODUCT = "Task_UpdateProductForm";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final BpmTaskCompleter bpmTaskCompleter;
    private final CurrentUserService currentUserService;

    public BpmAdminService(RuntimeService runtimeService,
                           TaskService taskService,
                           HistoryService historyService,
                           BpmTaskCompleter bpmTaskCompleter,
                           CurrentUserService currentUserService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.bpmTaskCompleter = bpmTaskCompleter;
        this.currentUserService = currentUserService;
    }

    public OrderBpmStartResponse startCreateProduct() {
        String login = camundaUserId();
        ProcessInstance existing = findActiveProductCreate(login);
        if (existing != null) {
            assignActiveTasks(existing.getId(), login);
            return buildStartResponse(existing, "Продолжите заполнение формы добавления товара в Tasklist");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("initiatorLogin", login);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_ADMIN_PRODUCT_CREATE, vars);
        assignActiveTasks(processInstance.getId(), login);
        return buildStartResponse(processInstance, "Заполните форму добавления товара в Camunda Tasklist");
    }

    public Long createProductSync(String name, double price) {
        ProcessInstance processInstance = bpmTaskCompleter.startAndCompleteUserTask(
                PROCESS_ADMIN_PRODUCT_CREATE,
                Map.of("initiatorLogin", camundaUserId()),
                TASK_CREATE_PRODUCT,
                Map.of("name", name, "price", price));
        Long productId = getHistoricLong(processInstance.getId(), "productId");
        if (productId == null) {
            throw new IllegalStateException("Product was not created by BPM process");
        }
        return productId;
    }

    public OrderBpmStartResponse startUpdateProduct(Long productId) {
        String login = camundaUserId();
        Map<String, Object> vars = new HashMap<>();
        vars.put("productId", productId);
        vars.put("initiatorLogin", login);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_ADMIN_PRODUCT_UPDATE, vars);
        Task formTask = bpmTaskCompleter.findActiveUserTask(processInstance.getId(), TASK_UPDATE_PRODUCT);
        if (formTask != null) {
            taskService.setVariable(formTask.getId(), "productId", productId);
        }
        assignActiveTasks(processInstance.getId(), login);
        return buildStartResponse(processInstance, "Измените данные товара в Camunda Tasklist");
    }

    public void updateProductSync(Long productId, String name, Double price) {
        Map<String, Object> formVars = new HashMap<>();
        formVars.put("productId", productId);
        if (name != null) {
            formVars.put("name", name);
        }
        if (price != null) {
            formVars.put("price", price);
        }
        bpmTaskCompleter.startAndCompleteUserTask(
                PROCESS_ADMIN_PRODUCT_UPDATE,
                Map.of("productId", productId, "initiatorLogin", camundaUserId()),
                TASK_UPDATE_PRODUCT,
                formVars);
    }

    private ProcessInstance findActiveProductCreate(String login) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_ADMIN_PRODUCT_CREATE)
                .variableValueEquals("initiatorLogin", login)
                .active()
                .singleResult();
    }

    private String camundaUserId() {
        return currentUserService.getCurrentUserLogin().toLowerCase();
    }

    private void assignActiveTasks(String processInstanceId, String login) {
        taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list()
                .forEach(task -> taskService.setAssignee(task.getId(), login));
    }

    private OrderBpmStartResponse buildStartResponse(ProcessInstance processInstance, String message) {
        Task activeTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .active()
                .singleResult();
        OrderBpmStartResponse response = new OrderBpmStartResponse();
        response.setProcessInstanceId(processInstance.getId());
        if (activeTask != null) {
            response.setTaskId(activeTask.getId());
            response.setTaskName(activeTask.getName());
        }
        response.setMessage(message);
        return response;
    }

    private Long getHistoricLong(String processInstanceId, String variableName) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        if (variable == null) {
            return null;
        }
        Object value = variable.getValue();
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        return null;
    }
}
