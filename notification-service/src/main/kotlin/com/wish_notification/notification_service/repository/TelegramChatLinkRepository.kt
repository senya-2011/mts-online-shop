package com.wish_notification.notification_service.repository

import com.wish_notification.notification_service.model.TelegramChatLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TelegramChatLinkRepository : JpaRepository<TelegramChatLink, Long> {
    fun findByTelegramUsernameIgnoreCase(telegramUsername: String): TelegramChatLink?
}


