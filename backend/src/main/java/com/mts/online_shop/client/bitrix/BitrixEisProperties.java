package com.mts.online_shop.client.bitrix;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bitrix")
public class BitrixEisProperties {

    private boolean enabled;
    private String webhookBase = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookBase() {
        return webhookBase;
    }

    public void setWebhookBase(String webhookBase) {
        this.webhookBase = webhookBase;
    }
}
