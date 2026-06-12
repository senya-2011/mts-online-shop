package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.model.ProductEntity;
import com.mts.online_shop.service.GoodsService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("createProductDelegate")
public class CreateProductDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CreateProductDelegate.class);

    private final GoodsService goodsService;

    public CreateProductDelegate(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String name = stringVar(execution, "name");
        if (name == null || name.isBlank()) {
            throw new BpmnError("VALIDATION_ERROR", "Не задано имя товара");
        }
        BigDecimal price = decimalVar(execution, "price");
        if (price == null) {
            throw new BpmnError("VALIDATION_ERROR", "Не задана цена товара");
        }
        ProductEntity product = goodsService.createProduct(name, price);
        execution.setVariable("productId", product.getId());
        log.info("BPM createProduct persisted id={} name={}", product.getId(), product.getName());
    }

    private static String stringVar(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value == null ? null : value.toString().trim();
    }

    private static BigDecimal decimalVar(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
