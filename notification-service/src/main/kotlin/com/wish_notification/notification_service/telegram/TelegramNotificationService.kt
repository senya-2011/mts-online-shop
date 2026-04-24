package com.wish_notification.notification_service.telegram

import com.dabwish.events.wish.WishNotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Service
class TelegramNotificationService(
    private val bot: TelegramBot,
    private val chatRegistry: TelegramChatRegistry,
    @Value("\${app.frontend.base-url:http://localhost:3000}") private val frontendBaseUrl: String,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun sendWishNotification(event: WishNotificationEvent) {
        val username = event.subscriberTelegramUsername
        val chatId = chatRegistry.getChatId(username)

        if (chatId == null) {
            log.info(
                "Skip wish notification for @{}: chatId not registered yet (user has not sent /start)",
                username
            )
            return
        }

        val wishUrl = "$frontendBaseUrl/wishes/${event.wishId}"
        val text = buildString {
            append("👤 <b>${event.ownerName}</b> создал новое желание: \"${event.wishTitle}\"")
            append("\n")
            append("<a href=\"$wishUrl\">Посмотреть желание</a>")
        }

        try {
            val message = SendMessage(chatId.toString(), text)

            message.parseMode = "HTML"

            message.disableWebPagePreview = false
            bot.execute(message)

            log.info("Sent wish notification...")
        } catch (e: Exception) {
            log.warn("Failed to send...", e)
        }
    }
}


