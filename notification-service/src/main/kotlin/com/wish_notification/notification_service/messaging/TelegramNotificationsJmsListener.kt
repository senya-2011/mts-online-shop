package com.wish_notification.notification_service.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.mts.messaging.contracts.TelegramNotificationEnvelope
import com.wish_notification.notification_service.telegram.TelegramBot
import com.wish_notification.notification_service.telegram.TelegramChatRegistry
import com.wish_notification.notification_service.telegram.TelegramNotificationService
import com.wish_notification.notification_service.telegram.TelegramVerificationStore
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Component
class TelegramNotificationsJmsListener(
    private val objectMapper: ObjectMapper,
    private val verificationStore: TelegramVerificationStore,
    private val chatRegistry: TelegramChatRegistry,
    private val bot: TelegramBot,
    private val telegramNotificationService: TelegramNotificationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @JmsListener(destination = "\${app.jms.telegram-queue:telegram.notifications}", containerFactory = "jmsListenerContainerFactory")
    fun onMessage(payload: String) {
        val env = try {
            objectMapper.readValue(payload, TelegramNotificationEnvelope::class.java)
        } catch (e: Exception) {
            log.warn("Skip invalid JMS payload: {}", e.message)
            return
        }
        log.info("JMS telegram notification type={} userId={} @{}", env.type(), env.userId(), env.telegramUsername())
        when (env.type()) {
            TelegramNotificationEnvelope.TYPE_VERIFICATION -> handleVerification(env)
            TelegramNotificationEnvelope.TYPE_PLAIN_TEXT -> telegramNotificationService.sendPlainText(env)
            else -> log.info("Unknown notification type: {}", env.type())
        }
    }

    private fun handleVerification(env: TelegramNotificationEnvelope) {
        val username = env.telegramUsername() ?: return
        val code = env.verificationCode() ?: return
        val userId = env.userId() ?: return

        val pending = TelegramVerificationStore.PendingVerification(userId, username, code)
        val chatId = chatRegistry.getChatId(username)
        if (chatId != null) {
            log.info("User @{} already registered (chatId={}), sending verification immediately", username, chatId)
            sendVerificationCode(chatId, username, code)
        } else {
            log.info("User @{} not registered yet, storing verification code for later", username)
            verificationStore.store(pending)
        }
    }

    private fun sendVerificationCode(chatId: Long, username: String, code: String) {
        val text = buildString {
            appendLine("Привет, @$username 👋")
            appendLine()
            append("Если вы не отправляли запрос, просто игнорируйте")
            appendLine()
            append("Твой код подтверждения Telegram в Online Shop: $code")
        }
        try {
            bot.execute(SendMessage(chatId.toString(), text.trim()))
            log.info("Sent verification code to chatId {} for @{}", chatId, username)
        } catch (e: Exception) {
            log.warn("Failed to send verification code to chatId {} for @{}: {}", chatId, username, e.message, e)
        }
    }
}
