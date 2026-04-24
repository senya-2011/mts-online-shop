package com.wish_notification.notification_service.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.io.Serializable

@Entity
@Table(name = "telegram_chat_links")
class TelegramChatLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @Column(name = "user_id", nullable = true)
    var userId: Long? = null,

    @Column(name = "telegram_username", nullable = false, unique = true, length = 255)
    var telegramUsername: String = "",

    @Column(name = "chat_id", nullable = false)
    var chatId: Long = 0,
): Serializable {
    constructor() : this(null, null, "", 0)
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass = if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false

        other as TelegramChatLink

        return id != 0L && id == other.id
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , username = $telegramUsername , chatid = $chatId)"
    }
}


