package com.wish_notification.notification_service.telegram

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class TelegramBot(
    @Value("\${app.telegram.bot-token}") private val botToken: String,
    @Value("\${app.telegram.bot-username}") private val botUsername: String,
    private val chatRegistry: TelegramChatRegistry,
    private val verificationStore: TelegramVerificationStore,
) : TelegramLongPollingBot(botToken) {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun getBotToken(): String = botToken

    @PostConstruct
    fun init() {
        log.info("=== TelegramBot initialized ===")

        try {
            log.info("Clearing webhook before starting polling...")
            val deleteWebhook = DeleteWebhook()
            deleteWebhook.dropPendingUpdates = true
            execute(deleteWebhook)
            log.info("Webhook cleared successfully!")
        } catch (e: Exception) {
            log.warn("Webhook clear attempt finished with message: {}", e.message)
        }

        log.info("Bot username: '{}'", botUsername)
        log.info("Bot token: '{}' (length: {})", 
            if (botToken.length > 10) "${botToken.take(10)}..." else "***", 
            botToken.length)
        log.info("Bot token is empty or default: {}", botToken.isBlank() || botToken == "CHANGE_ME")
        log.info("Bot username is empty or default: {}", botUsername.isBlank() || botUsername == "dabwish_bot")
    }

    @PreDestroy
    fun onShutdown() {
        try {
            log.info("Shutting down TelegramBot, deleting webhook if any...")
            execute(DeleteWebhook())
            log.info("Webhook deleted successfully (if it existed)")
        } catch (e: Exception) {
            log.warn("Failed to delete webhook on shutdown: {}", e.message, e)
        }
    }

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        log.info("=== TelegramBot received update: updateId={}", update.updateId)
        
        val message = update.message
        if (message == null) {
            log.info("Update has no message, ignoring")
            return
        }

        val chatId = message.chatId
        val text = message.text ?: ""
        val from = message.from
        val username = from?.userName
        val firstName = from?.firstName
        val lastName = from?.lastName

        log.info(
            "Received message: chatId={}, text='{}', username={}, firstName={}, lastName={}",
            chatId, text, username, firstName, lastName
        )

        if (username.isNullOrBlank()) {
            log.warn("Received message in chatId {} without username, ignoring. User: {} {}", chatId, firstName, lastName)
            return
        }

        if (text.startsWith("/start")) {
            log.info("Processing /start command for @{} (chatId={})", username, chatId)
            handleStart(chatId, username)
        } else {
            log.info("Message is not /start, ignoring. Text: '{}'", text)
        }
    }

    private fun handleStart(chatId: Long, username: String) {
        log.info("=== handleStart called: chatId={}, username=@{}", chatId, username)
        
        try {
            chatRegistry.register(username, chatId, null)
            log.info("Registered chatId {} for @{} in database", chatId, username)
        } catch (e: Exception) {
            log.error("Failed to register chatId {} for @{}: {}", chatId, username, e.message, e)
            throw e
        }

        val pending = verificationStore.consume(username)
        log.info("Consumed pending verification code for @{}: {}", username, if (pending != null) "FOUND" else "NOT FOUND")
        val welcomeText = buildString {
            appendLine("Привет, @$username 👋")
            appendLine("Теперь я смогу присылать тебе уведомления о новых желаниях.")
            if (pending != null) {
                val normalizedTelegramUsername = normalize(username)
                val normalizedEventUsername = normalize(pending.telegramUsername)
                
                if (normalizedTelegramUsername == normalizedEventUsername) {
                    appendLine()
                    append("Если вы не отправляли запрос, просто игнорируйте\nТвой код подтверждения Telegram в DabWish: ")
                    append(pending.verificationCode)
                    log.info(
                        "Sent verification code to @{} (userId={}) - username matches",
                        username,
                        pending.userId
                    )
                } else {
                    log.warn(
                        "Username mismatch: Telegram username @{} does not match event username @{} (userId={}). Code not sent.",
                        username,
                        pending.telegramUsername,
                        pending.userId
                    )
                }
            }
        }.trim()

        try {
            val message = SendMessage(chatId.toString(), welcomeText)
            execute(message)
            log.info("Sent welcome message to chatId {} for @{}", chatId, username)
        } catch (e: Exception) {
            log.warn(
                "Failed to send /start welcome message to chatId {} for @{}: {}",
                chatId,
                username,
                e.message,
                e
            )
        }
    }

    private fun normalize(username: String): String =
        username.removePrefix("@").trim().lowercase()
}


