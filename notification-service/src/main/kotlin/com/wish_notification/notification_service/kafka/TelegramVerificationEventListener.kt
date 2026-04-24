package com.wish_notification.notification_service.kafka

import com.dabwish.events.telegram.TelegramVerificationCodeEvent
import com.wish_notification.notification_service.telegram.TelegramBot
import com.wish_notification.notification_service.telegram.TelegramChatRegistry
import com.wish_notification.notification_service.telegram.TelegramVerificationStore
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Component
@ConditionalOnProperty(value = ["app.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class TelegramVerificationEventListener(
    private val verificationStore: TelegramVerificationStore,
    private val chatRegistry: TelegramChatRegistry,
    private val bot: TelegramBot,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(
        topics = ["\${app.kafka.topics.telegram-verification-code:telegram-verification-code-events}"],
    )
    fun handleTelegramVerificationCode(event: TelegramVerificationCodeEvent) {
        log.info(
            "Received Telegram verification event for @{} [User ID: {}]",
            event.telegramUsername,
            event.userId
        )

        val chatId = chatRegistry.getChatId(event.telegramUsername)
        
        if (chatId != null) {
            log.info(
                "User @{} already registered (chatId={}), sending verification code immediately",
                event.telegramUsername,
                chatId
            )
            sendVerificationCode(chatId, event.telegramUsername, event.verificationCode)
        } else {
            log.info(
                "User @{} not registered yet, storing verification code for later",
                event.telegramUsername
            )
            verificationStore.store(event)
        }
    }

    private fun sendVerificationCode(chatId: Long, username: String, code: String) {
        val text = buildString {
            appendLine("Привет, @$username 👋")
            appendLine()
            append("Если вы не отправляли запрос, просто игнорируйте")
            appendLine()
            append("Твой код подтверждения Telegram в DabWish: $code")
        }

        try {
            val message = SendMessage(chatId.toString(), text.trim())
            bot.execute(message)
            log.info("Sent verification code to chatId {} for @{}", chatId, username)
        } catch (e: Exception) {
            log.warn(
                "Failed to send verification code to chatId {} for @{}: {}",
                chatId,
                username,
                e.message,
                e
            )
        }
    }
}

