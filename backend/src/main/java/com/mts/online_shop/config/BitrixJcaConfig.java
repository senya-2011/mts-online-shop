package com.mts.online_shop.config;

import com.mts.online_shop.bank.jca.LocalConnectionManager;
import com.mts.online_shop.bitrix.jca.BitrixManagedConnectionFactory;
import com.mts.online_shop.client.bitrix.BitrixEisProperties;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class BitrixJcaConfig {

    @Bean
    public BitrixManagedConnectionFactory bitrixManagedConnectionFactory(BitrixEisProperties properties) {
        BitrixManagedConnectionFactory factory = new BitrixManagedConnectionFactory();
        factory.setWebhookBaseUrl(properties.getWebhookBase());
        return factory;
    }

    @Bean(name = "bitrixJcaConnectionFactory")
    public ConnectionFactory bitrixJcaConnectionFactory(
            BitrixManagedConnectionFactory managedConnectionFactory,
            LocalConnectionManager connectionManager
    ) throws ResourceException {
        return (ConnectionFactory) managedConnectionFactory.createConnectionFactory(connectionManager);
    }
}
