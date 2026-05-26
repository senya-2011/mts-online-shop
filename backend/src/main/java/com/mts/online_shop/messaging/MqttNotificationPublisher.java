package com.mts.online_shop.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mts.messaging.contracts.TelegramNotificationEnvelope;
import com.mts.online_shop.config.MqttProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class MqttNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttNotificationPublisher.class);

    private final MqttProperties mqttProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MqttClient mqttClient;

    public MqttNotificationPublisher(MqttProperties mqttProperties) {
        this.mqttProperties = mqttProperties;
    }

    @PostConstruct
    public void connect() {
        try {
            String clientId = mqttProperties.getClientIdPrefix() + "-" + UUID.randomUUID().toString().substring(0, 8);
            mqttClient = new MqttClient(mqttProperties.getBrokerUri(), clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            mqttClient.connect(options);
            log.info("MQTT connected to {} as {}", mqttProperties.getBrokerUri(), clientId);
        } catch (MqttException e) {
            log.warn("MQTT connect failed (notifications disabled until broker is up): {}", e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                log.debug("MQTT disconnect: {}", e.getMessage());
            }
        }
    }

    public void publish(TelegramNotificationEnvelope envelope) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("MQTT not connected; skip publish type={}", envelope.type());
            return;
        }
        try {
            byte[] payload = objectMapper.writeValueAsString(envelope).getBytes(StandardCharsets.UTF_8);
            mqttClient.publish(mqttProperties.getTopic(), payload, 0, false);
            log.debug("Published MQTT notification type={} userId={}", envelope.type(), envelope.userId());
        } catch (Exception e) {
            log.error("MQTT publish failed: {}", e.getMessage());
        }
    }
}
