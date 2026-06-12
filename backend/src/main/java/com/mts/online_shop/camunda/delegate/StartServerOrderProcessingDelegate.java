package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmUserIdResolver;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("startServerOrderProcessingDelegate")
public class StartServerOrderProcessingDelegate implements JavaDelegate {

    public static final String PROCESS_SERVER_ORDER_PROCESSING = "server-order-processing";

    private final RuntimeService runtimeService;
    private final BpmUserIdResolver bpmUserIdResolver;

    public StartServerOrderProcessingDelegate(
            RuntimeService runtimeService,
            BpmUserIdResolver bpmUserIdResolver) {
        this.runtimeService = runtimeService;
        this.bpmUserIdResolver = bpmUserIdResolver;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = DelegateVariables.requiredLong(execution, "orderId", "orderId обязателен после оплаты.");
        Long userId = bpmUserIdResolver.requiredUserId(execution);
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderId", orderId);
        vars.put("userId", userId);
        runtimeService.startProcessInstanceByKey(PROCESS_SERVER_ORDER_PROCESSING, vars);
    }
}
