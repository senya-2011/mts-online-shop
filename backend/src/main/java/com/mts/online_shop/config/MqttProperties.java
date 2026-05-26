package com.mts.online_shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mqtt")
public class MqttProperties {

    /**
     * Eclipse Paho broker URI, e.g. tcp://rabbitmq:1883
     */
    private String brokerUri = "tcp://localhost:1883";

    private String clientIdPrefix = "online-shop";

    /**
     * MQTT topic published to RabbitMQ (maps to amq.topic routing key with dots).
     */
    private String topic = "mts/shop/telegram/notify";

    public String getBrokerUri() {
        return brokerUri;
    }

    public void setBrokerUri(String brokerUri) {
        this.brokerUri = brokerUri;
    }

    public String getClientIdPrefix() {
        return clientIdPrefix;
    }

    public void setClientIdPrefix(String clientIdPrefix) {
        this.clientIdPrefix = clientIdPrefix;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
