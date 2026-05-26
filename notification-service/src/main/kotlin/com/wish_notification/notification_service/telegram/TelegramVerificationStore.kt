package com.wish_notification.notification_service.telegram

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

@Component
class TelegramVerificationStore {

    private val log = LoggerFactory.getLogger(javaClass)

    data class PendingVerification(
        val userId: Long,
        val telegramUsername: String,
        val verificationCode: String,
    )

    private data class StoredEvent(
        val event: PendingVerification,
        val storedAt: Instant,
    )

    private val pendingByUsername: MutableMap<String, StoredEvent> = ConcurrentHashMap()

    fun store(event: PendingVerification) {
        val normalized = normalize(event.telegramUsername)
        pendingByUsername[normalized] = StoredEvent(event, Instant.now())
        log.info(
            "Stored pending Telegram verification code for @{} (userId={}), expires in 1 hour",
            normalized,
            event.userId,
        )
    }

    fun consume(username: String): PendingVerification? {
        val normalized = normalize(username)
        val stored = pendingByUsername.remove(normalized) ?: return null

        val age = ChronoUnit.HOURS.between(stored.storedAt, Instant.now())
        if (age >= 1) {
            log.info(
                "Verification code for @{} expired (age: {} hours), not returning",
                normalized,
                age,
            )
            return null
        }

        log.info(
            "Consumed pending Telegram verification code for @{} (userId={})",
            normalized,
            stored.event.userId,
        )
        return stored.event
    }

    @Scheduled(fixedRate = 3600000)
    fun cleanupExpired() {
        val now = Instant.now()
        val expired = pendingByUsername.entries.filter { entry ->
            ChronoUnit.HOURS.between(entry.value.storedAt, now) >= 1
        }

        expired.forEach { entry ->
            pendingByUsername.remove(entry.key)
            log.debug(
                "Cleaned up expired verification code for @{} (userId={})",
                entry.key,
                entry.value.event.userId,
            )
        }

        if (expired.isNotEmpty()) {
            log.info("Cleaned up {} expired verification codes", expired.size)
        }
    }

    private fun normalize(username: String): String =
        username.removePrefix("@").trim().lowercase()
}
