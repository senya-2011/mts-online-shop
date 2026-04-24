package com.wish_notification.notification_service.config

import com.rabbitmq.jms.admin.RMQConnectionFactory
import jakarta.jms.ConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.support.converter.MessageConverter
import org.springframework.jms.support.converter.SimpleMessageConverter

@Configuration
@EnableJms
class JmsConfig {

    @Bean
    fun jmsConnectionFactory(
        @Value("\${spring.rabbitmq.host:localhost}") host: String,
        @Value("\${spring.rabbitmq.port:5672}") port: Int,
        @Value("\${spring.rabbitmq.username:guest}") username: String,
        @Value("\${spring.rabbitmq.password:guest}") password: String,
    ): ConnectionFactory {
        val factory = RMQConnectionFactory()
        factory.setHost(host)
        factory.setPort(port)
        factory.setUsername(username)
        factory.setPassword(password)
        return factory
    }

    @Bean
    fun jmsMessageConverter(): MessageConverter = SimpleMessageConverter()

    @Bean
    fun jmsListenerContainerFactory(
        jmsConnectionFactory: ConnectionFactory,
        jmsMessageConverter: MessageConverter,
    ): DefaultJmsListenerContainerFactory {
        val factory = DefaultJmsListenerContainerFactory()
        factory.setConnectionFactory(jmsConnectionFactory)
        factory.setMessageConverter(jmsMessageConverter)
        factory.setConcurrency("1-2")
        factory.setSessionTransacted(false)
        return factory
    }
}
