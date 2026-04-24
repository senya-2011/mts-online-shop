package com.wish_notification.notification_service.config

import com.wish_notification.notification_service.telegram.TelegramBot
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.TelegramBotsApi

/**
 * Registers long-polling after the app is up. At cold start Docker/network may not reach
 * api.telegram.org yet; [BotConfig] used to swallow failures so the bot never polled.
 */
@Component
@Order(Int.MAX_VALUE / 2)
class TelegramBotRegistrationRunner(
    private val telegramBotsApi: TelegramBotsApi,
    private val bot: TelegramBot,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val maxAttempts = 120
        val sleepMs = 5_000L
        repeat(maxAttempts) { attempt ->
            try {
                telegramBotsApi.registerBot(bot)
                log.info("--- TELEGRAM BOT REGISTERED SUCCESSFULLY (attempt {}) ---", attempt + 1)
                return
            } catch (e: Exception) {
                log.warn(
                    "Telegram registerBot failed (attempt {}/{}): {} — /start will not work until registration succeeds; " +
                        "from the container, HTTPS to api.telegram.org must work (VPN/firewall/Docker DNS).",
                    attempt + 1,
                    maxAttempts,
                    e.message,
                )
                if (attempt < maxAttempts - 1) {
                    Thread.sleep(sleepMs)
                }
            }
        }
        log.error(
            "Telegram bot was not registered after {} attempts (~{} min).",
            maxAttempts,
            maxAttempts * sleepMs / 60_000,
        )
    }
}
