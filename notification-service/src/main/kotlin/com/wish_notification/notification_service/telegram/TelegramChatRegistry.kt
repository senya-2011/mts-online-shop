package com.wish_notification.notification_service.telegram

import com.wish_notification.notification_service.model.TelegramChatLink
import com.wish_notification.notification_service.repository.TelegramChatLinkRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TelegramChatRegistry(
    private val repository: TelegramChatLinkRepository,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun register(username: String, chatId: Long, userId: Long? = null) {
        val normalized = normalize(username)
        val existing = repository.findByTelegramUsernameIgnoreCase(normalized)

        val entity = if (existing != null) {
            existing.chatId = chatId
            if (userId != null) {
                existing.userId = userId
            }
            existing
        } else {
            TelegramChatLink(
                telegramUsername = normalized,
                chatId = chatId,
                userId = userId,
            )
        }

        repository.save(entity)
        log.info("Registered Telegram chatId {} for username @{}", chatId, normalized)
    }

    @Transactional(readOnly = true)
    fun getChatId(username: String): Long? {
        val normalized = normalize(username)
        return repository.findByTelegramUsernameIgnoreCase(normalized)?.chatId
    }

    private fun normalize(username: String): String =
        username.removePrefix("@").trim().lowercase()
}

