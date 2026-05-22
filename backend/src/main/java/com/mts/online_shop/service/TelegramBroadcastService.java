package com.mts.online_shop.service;

import com.mts.messaging.contracts.TelegramNotificationEnvelope;
import com.mts.online_shop.messaging.MqttNotificationPublisher;
import com.mts.online_shop.model.UserTelegramLink;
import com.mts.online_shop.repository.UserTelegramLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TelegramBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBroadcastService.class);

    private static final String[] SALE_WORDS = {
            "скидки", "халява", "минус50", "финал", "горящее", "успей", "сегодня",
            "бонус", "подарок", "выгода", "только-сейчас", "mega-deal"
    };

    private final UserTelegramLinkRepository telegramLinkRepository;
    private final MqttNotificationPublisher mqttNotificationPublisher;

    public TelegramBroadcastService(
            UserTelegramLinkRepository telegramLinkRepository,
            MqttNotificationPublisher mqttNotificationPublisher
    ) {
        this.telegramLinkRepository = telegramLinkRepository;
        this.mqttNotificationPublisher = mqttNotificationPublisher;
    }

    /**
     * Публикует в RabbitMQ (MQTT) рассылку всем привязанным Telegram-аккаунтам.
     *
     * @param messagesPerUser сколько сообщений на каждого привязанного пользователя (минимум 1)
     * @param delayMs         пауза между сообщениями, чтобы было видно на дашборде RabbitMQ
     */
    public int broadcastSale(int messagesPerUser, long delayMs) {
        List<UserTelegramLink> links = telegramLinkRepository.findAllWithUserOrderByLogin();
        if (links.isEmpty()) {
            throw new IllegalStateException("Нет привязанных Telegram-аккаунтов");
        }

        int perUser = Math.max(1, messagesPerUser);
        int published = 0;

        for (UserTelegramLink link : links) {
            String username = link.getTelegramUsername();
            Long userId = link.getUser().getId();
            for (int i = 0; i < perUser; i++) {
                String text = "Распродажа " + randomWord();
                mqttNotificationPublisher.publish(new TelegramNotificationEnvelope(
                        TelegramNotificationEnvelope.TYPE_PLAIN_TEXT,
                        userId,
                        username,
                        text,
                        null
                ));
                published++;
                log.info("Broadcast MQTT message #{} to @{}: {}", published, username, text);
                sleep(delayMs);
            }
        }
        return published;
    }

    private static String randomWord() {
        return SALE_WORDS[ThreadLocalRandom.current().nextInt(SALE_WORDS.length)];
    }

    private static void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
