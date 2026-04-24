package com.mts.online_shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration
public class NarayanaJtaConfig {

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        JtaTransactionManager transactionManager = new JtaTransactionManager();
        transactionManager.setTransactionManager(com.arjuna.ats.jta.TransactionManager.transactionManager());
        transactionManager.setUserTransaction(com.arjuna.ats.jta.UserTransaction.userTransaction());
        return transactionManager;
    }

    @Bean
    public jakarta.transaction.TransactionManager narayanaTransactionManager() {
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }
}
