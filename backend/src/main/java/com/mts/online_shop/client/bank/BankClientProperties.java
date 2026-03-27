package com.mts.online_shop.client.bank;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки подключения к банку. Берутся из env (например .env):
 * BANK_URL — базовый URL банка, например http://localhost:8081
 */
@ConfigurationProperties(prefix = "bank")
public class BankClientProperties {

    /**
     * Базовый URL банковского сервиса (без /api на конце).
     */
    private String baseUrl = "http://localhost:8081";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
