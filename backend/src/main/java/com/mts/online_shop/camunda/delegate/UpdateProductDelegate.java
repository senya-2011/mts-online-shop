package com.mts.online_shop.camunda.delegate;

import com.mts.online_shop.model.ProductEntity;
import com.mts.online_shop.service.GoodsService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("updateProductDelegate")
public class UpdateProductDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(UpdateProductDelegate.class);

    private final GoodsService goodsService;

    public UpdateProductDelegate(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long productId = DelegateVariables.requiredLong(execution, "productId", "Передайте productId из API.");
        String name = stringVar(execution, "name");
        BigDecimal price = decimalVar(execution, "price");
        if ((name == null || name.isBlank()) && price == null) {
            throw new IllegalStateException("Укажите новое название и/или цену товара");
        }
        ProductEntity updated = goodsService.updateProduct(productId, name, price);
        log.info("BPM updateProduct persisted id={} name={} price={}", updated.getId(), updated.getName(), updated.getPrice());
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
