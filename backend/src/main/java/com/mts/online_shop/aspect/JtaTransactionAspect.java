package com.mts.online_shop.aspect;

import com.arjuna.ats.jta.common.jtaPropertyManager;
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
public class NarayanaTransactionAspect {

    private static final Logger log = LoggerFactory.getLogger(NarayanaTransactionAspect.class);

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object aroundNarayanaTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        boolean isTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        
        // Get Narayana transaction info
        String narayanaTxId = null;
        try {
            if (isTransactionActive) {
                narayanaTxId = com.arjuna.ats.arjuna.coordinator.TxControl.getCurrentTx().get_uid().toString();
            }
        } catch (Exception e) {
            // Ignore if transaction not available
        }
        
        log.info("Starting Narayana transaction - {}.{} (Active: {}, Transaction: {}, NarayanaTxId: {})", 
                className, methodName, isTransactionActive, transactionName, narayanaTxId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Narayana transaction completed successfully - {}.{} (Duration: {}ms, NarayanaTxId: {})", 
                    className, methodName, duration, narayanaTxId);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Narayana transaction failed - {}.{} (Duration: {}ms, Error: {}, NarayanaTxId: {})", 
                    className, methodName, duration, e.getMessage(), narayanaTxId);
            throw e;
        }
    }
}
