package com.mts.online_shop.camunda;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Shop DB work from Camunda delegates runs in a separate JTA transaction (WildFly-safe).
 */
@Component
public class BpmDelegateSupport {

    private final TransactionTemplate requiresNewTx;

    public BpmDelegateSupport(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiresNewTx = template;
    }

    public void runInNewTransaction(Runnable action) {
        requiresNewTx.executeWithoutResult(status -> action.run());
    }

    public <T> T callInNewTransaction(Supplier<T> action) {
        return requiresNewTx.execute(status -> action.get());
    }
}
