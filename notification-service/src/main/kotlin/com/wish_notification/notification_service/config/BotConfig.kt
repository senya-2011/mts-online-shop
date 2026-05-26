package com.wish_notification.notification_service.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Configuration
class BotConfig {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun telegramBotsApi(): TelegramBotsApi {
        log.info("TelegramBotsApi created; bot registration runs in TelegramBotRegistrationRunner")
        return TelegramBotsApi(DefaultBotSession::class.java)
    }
}