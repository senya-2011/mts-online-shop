package com.wish_notification.notification_service.kafka

import com.dabwish.events.wish.WishCreatedEvent
import com.dabwish.events.wish.WishNotificationEvent
import com.dabwish.events.wish.WishUpdatedEvent
import com.wish_notification.notification_service.telegram.TelegramNotificationService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = ["app.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class WishEventListener(
    private val telegramNotificationService: TelegramNotificationService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(
        topics = ["\${app.kafka.topics.wish-created:wish-created-events}"],
    )
    fun handleWishCreated(event: WishCreatedEvent) {
        log.info(
            "New Wish Created! [ID: {}] Owner: {}, Title: '{}', Price: {}, CreatedAt: {}",
            event.wishId,
            event.ownerId,
            event.title,
            event.price ?: "N/A",
            event.createdAt
        )

    }

    @KafkaListener(
        topics = ["\${app.kafka.topics.wish-updated:wish-updated-events}"],
    )
    fun handleWishUpdated(event: WishUpdatedEvent) {
        log.info(
            "Wish Updated [ID: {}] Owner: {}. Changed: Title='{}', Price={}, UpdatedAt={}",
            event.wishId,
            event.ownerId,
            event.title ?: "-",
            event.price ?: "-",
            event.updatedAt
        )
    }

    @KafkaListener(
        topics = ["\${app.kafka.topics.wish-notification:wish-notification-events}"],
    )
    fun handleWishNotification(event: WishNotificationEvent) {
        log.info(
            "Уведомление для @{}: пользователь {} создал новое желание '{}' [Wish ID: {}]",
            event.subscriberTelegramUsername,
            event.ownerName,
            event.wishTitle,
            event.wishId
        )
        telegramNotificationService.sendWishNotification(event)
    }
}
