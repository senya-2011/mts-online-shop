package com.mts.online_shop.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.transaction.TransactionManager;

@Component
public class NarayanaUtil {

    private static final Logger log = LoggerFactory.getLogger(NarayanaUtil.class);

    public String getCurrentTransactionId() {
        try {
            TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
            if (tm != null && tm.getTransaction() != null) {
                return tm.getTransaction().toString();
            }
        } catch (Exception e) {
            log.debug("Unable to get current transaction ID: {}", e.getMessage());
        }
        return null;
    }

    public boolean isTransactionActive() {
        try {
            TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
            return tm != null && tm.getTransaction() != null;
        } catch (Exception e) {
            log.debug("Unable to check transaction status: {}", e.getMessage());
            return false;
        }
    }

    public String getTransactionStatus() {
        try {
            TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
            if (tm != null && tm.getTransaction() != null) {
                return tm.getTransaction().toString();
            }
        } catch (Exception e) {
            log.debug("Unable to get transaction status: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    public void logNarayanaConfiguration() {
        try {
            log.info("=== Narayana JTA Configuration ===");
            log.info("Narayana Transaction Manager: {}", com.arjuna.ats.jta.TransactionManager.transactionManager());
            log.info("Narayana User Transaction: {}", com.arjuna.ats.jta.UserTransaction.userTransaction());
            log.info("=====================================");
        } catch (Exception e) {
            log.error("Failed to log Narayana configuration", e);
        }
    }
}
