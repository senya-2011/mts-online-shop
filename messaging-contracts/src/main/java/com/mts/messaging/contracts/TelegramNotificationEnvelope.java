package com.mts.messaging.contracts;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * JSON envelope published over MQTT and consumed via JMS from RabbitMQ.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramNotificationEnvelope(
        String type,
        Long userId,
        String telegramUsername,
        String text,
        String verificationCode
) {
    public static final String TYPE_PLAIN_TEXT = "PLAIN_TEXT";
    public static final String TYPE_VERIFICATION = "VERIFICATION";
}
