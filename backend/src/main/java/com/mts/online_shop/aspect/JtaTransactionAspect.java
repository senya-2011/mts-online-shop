package com.mts.online_shop.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
public class JtaTransactionAspect {

    private static final Logger log = LoggerFactory.getLogger(JtaTransactionAspect.class);

    @Around("@annotation(jakarta.transaction.Transactional)")
    public Object aroundJtaTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        boolean isTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        
        log.info("Starting JTA transaction - {}.{} (Active: {}, Transaction: {})", 
                className, methodName, isTransactionActive, transactionName);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("JTA transaction completed successfully - {}.{} (Duration: {}ms)", 
                    className, methodName, duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("JTA transaction failed - {}.{} (Duration: {}ms, Error: {})", 
                    className, methodName, duration, e.getMessage());
            throw e;
        }
    }
}
