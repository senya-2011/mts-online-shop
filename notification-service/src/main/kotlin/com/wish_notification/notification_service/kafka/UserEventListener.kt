package com.wish_notification.notification_service.kafka

import com.dabwish.events.user.UserCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = ["app.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class UserEventListener {

    private val log = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(topics = ["\${app.kafka.topics.user-created:user-created-events}"])
    fun handleUserCreated(event: UserCreatedEvent) {
        log.info(
            "Received UserCreatedEvent: id={}, name={}, role={}, createdAt={}",
            event.userId,
            event.name,
            event.role,
            event.createdAt
        )
    }
}

