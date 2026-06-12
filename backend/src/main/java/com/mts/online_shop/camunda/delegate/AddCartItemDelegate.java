package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.camunda.BpmDelegateSupport;
import com.mts.online_shop.camunda.BpmUserIdResolver;
import com.mts.online_shop.exception.ProductNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.service.GoodsService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("addCartItemDelegate")
public class AddCartItemDelegate implements JavaDelegate {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final GoodsService goodsService;
    private final BpmUserIdResolver bpmUserIdResolver;
    private final BpmDelegateSupport bpmDelegateSupport;

    public AddCartItemDelegate(
            GoodsService goodsService,
            BpmUserIdResolver bpmUserIdResolver,
            BpmDelegateSupport bpmDelegateSupport) {
        this.goodsService = goodsService;
        this.bpmUserIdResolver = bpmUserIdResolver;
        this.bpmDelegateSupport = bpmDelegateSupport;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long userId = bpmUserIdResolver.requiredUserId(execution);
        Long productId = DelegateVariables.requiredLong(execution, "productId", "Передайте productId из API.");
        try {
            bpmDelegateSupport.runInNewTransaction(() -> goodsService.addProductInUserCart(userId, productId));
        } catch (UserNotFoundException ex) {
            throw validationError("Пользователь не найден в базе магазина. Перезайдите в Tasklist и начните процесс заново.");
        } catch (ProductNotFoundException ex) {
            throw validationError("Товар с ID " + productId + " не найден. Укажите существующий ID товара.");
        }
    }

    private static BpmnError validationError(String message) {
        return new BpmnError(VALIDATION_ERROR, message);
    }
}
