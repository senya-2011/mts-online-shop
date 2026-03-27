package com.mts.online_shop.util;

import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

@Component
public class NarayanaUtil {

    private static final Logger log = LoggerFactory.getLogger(NarayanaUtil.class);

    public String getCurrentTransactionId() {
        try {
            if (TxControl.getCurrentTx() != null) {
                return TxControl.getCurrentTx().get_uid().toString();
            }
        } catch (Exception e) {
            log.debug("Unable to get current transaction ID: {}", e.getMessage());
        }
        return null;
    }

    public boolean isTransactionActive() {
        try {
            return TxControl.getCurrentTx() != null && !TxControl.getCurrentTx().equals(TxControl.getNullTx());
        } catch (Exception e) {
            return false;
        }
    }

    public int getTransactionStatus() {
        try {
            if (TxControl.getCurrentTx() != null) {
                return TxControl.getCurrentTx().getStatus();
            }
        } catch (Exception e) {
            log.debug("Unable to get transaction status: {}", e.getMessage());
        }
        return -1;
    }

    public void logNarayanaConfiguration() {
        log.info("Narayana Transaction Manager Configuration:");
        log.info("  Node Identifier: {}", jtaPropertyManager.getJTAEnvironmentBean().getNodeIdentifier());
        log.info("  Transaction Timeout: {}s", jtaPropertyManager.getJTAEnvironmentBean().getDefaultTimeout());
        log.info("  Object Store Dir: {}", jtaPropertyManager.getJTAEnvironmentBean().getObjectStoreDir());
        log.info("  Recovery Period: {}s", jtaPropertyManager.getJTAEnvironmentBean().getPeriodicRecoveryPeriod());
        log.info("  Status Port: {}", jtaPropertyManager.getJTAEnvironmentBean().getStatusPort());
    }
}
