package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmDelegateSupport;
import com.mts.online_shop.camunda.BpmUserIdResolver;
import com.mts.online_shop.service.GoodsService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("validateCartDelegate")
public class ValidateCartDelegate implements JavaDelegate {

    private final GoodsService goodsService;
    private final BpmUserIdResolver bpmUserIdResolver;
    private final BpmDelegateSupport bpmDelegateSupport;

    public ValidateCartDelegate(
            GoodsService goodsService,
            BpmUserIdResolver bpmUserIdResolver,
            BpmDelegateSupport bpmDelegateSupport) {
        this.goodsService = goodsService;
        this.bpmUserIdResolver = bpmUserIdResolver;
        this.bpmDelegateSupport = bpmDelegateSupport;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Object existingOrderId = execution.getVariable("orderId");
        if (existingOrderId != null) {
            return;
        }
        Long userId = bpmUserIdResolver.requiredUserId(execution);
        if (bpmDelegateSupport.callInNewTransaction(() -> goodsService.findUserGoods(userId)).isEmpty()) {
            throw new BpmnError("VALIDATION_ERROR", "Корзина пуста. Добавьте товар перед оформлением заказа.");
        }
    }
}
