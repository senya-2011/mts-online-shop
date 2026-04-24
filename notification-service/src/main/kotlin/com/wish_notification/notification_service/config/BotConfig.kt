package com.wish_notification.notification_service.config

import com.wish_notification.notification_service.telegram.TelegramBot
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Configuration
class BotConfig {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun telegramBotsApi(bot: TelegramBot): TelegramBotsApi {
        log.info("--- FORCE STARTING TELEGRAM BOT ---")
        val api = TelegramBotsApi(DefaultBotSession::class.java)
        try {
            api.registerBot(bot)
            log.info("--- TELEGRAM BOT REGISTERED SUCCESSFULLY ---")
        } catch (e: Exception) {
            log.error("Failed to register bot manually", e)
        }
        return api
    }
}