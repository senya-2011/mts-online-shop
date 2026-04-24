package com.mts.online_shop.config;

import com.mts.online_shop.bank.jca.BankManagedConnectionFactory;
import com.mts.online_shop.bank.jca.LocalConnectionManager;
import com.mts.online_shop.client.bank.BankClientProperties;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BankJcaConfig {

    @Bean
    public LocalConnectionManager bankLocalConnectionManager() {
        return new LocalConnectionManager();
    }

    @Bean
    public BankManagedConnectionFactory bankManagedConnectionFactory(BankClientProperties properties) {
        BankManagedConnectionFactory factory = new BankManagedConnectionFactory();
        factory.setBankBaseUrl(properties.getBaseUrl());
        return factory;
    }

    @Bean(name = "bankJcaConnectionFactory")
    public ConnectionFactory bankJcaConnectionFactory(
            BankManagedConnectionFactory managedConnectionFactory,
            LocalConnectionManager connectionManager
    ) throws ResourceException {
        return (ConnectionFactory) managedConnectionFactory.createConnectionFactory(connectionManager);
    }
}
