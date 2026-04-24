package com.wish_notification.notification_service.telegram

import com.mts.messaging.contracts.TelegramNotificationEnvelope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Service
class TelegramNotificationService(
    private val bot: TelegramBot,
    private val chatRegistry: TelegramChatRegistry,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun sendPlainText(env: TelegramNotificationEnvelope) {
        val username = env.telegramUsername() ?: return
        val text = env.text() ?: return
        val chatId = chatRegistry.getChatId(username)
        if (chatId == null) {
            log.info("Skip plain text for @{}: chatId not registered (/start)", username)
            return
        }
        try {
            val message = SendMessage(chatId.toString(), text)
            message.parseMode = ParseMode.HTML
            bot.execute(message)
            log.info("Sent plain text notification to @{}", username)
        } catch (e: Exception) {
            log.warn("Failed to send plain text to @{}: {}", username, e.message)
        }
    }
}
