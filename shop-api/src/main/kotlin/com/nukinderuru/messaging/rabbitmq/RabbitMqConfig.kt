package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.events.ProductEventMessaging
import com.rabbitmq.client.ConnectionFactory
import io.ktor.server.config.ApplicationConfig

data class RabbitMqConfig(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val virtualHost: String,
    val exchange: String,
    val routingKey: String,
    val mainQueue: String,
    val retryExchange: String,
    val deadLetterExchange: String,
    val deadLetterQueue: String,
    val confirmTimeoutMillis: Long,
    val maxRetryAttempts: Int = ProductEventMessaging.MAX_RETRY_ATTEMPTS
) {
    fun createConnectionFactory(): ConnectionFactory = ConnectionFactory().apply {
        host = this@RabbitMqConfig.host
        port = this@RabbitMqConfig.port
        username = this@RabbitMqConfig.username
        password = this@RabbitMqConfig.password
        virtualHost = this@RabbitMqConfig.virtualHost
    }

    companion object {
        fun from(config: ApplicationConfig): RabbitMqConfig = RabbitMqConfig(
            enabled = config.value("rabbitmq.enabled", "false").toBoolean(),
            host = config.value("rabbitmq.host", "localhost"),
            port = config.value("rabbitmq.port", "5672").toInt(),
            username = config.value("rabbitmq.username", "guest"),
            password = config.value("rabbitmq.password", "guest"),
            virtualHost = config.value("rabbitmq.virtualHost", "/"),
            exchange = config.value("rabbitmq.exchange", ProductEventMessaging.EXCHANGE),
            routingKey = config.value("rabbitmq.routingKey", ProductEventMessaging.ROUTING_KEY),
            mainQueue = config.value("rabbitmq.mainQueue", ProductEventMessaging.MAIN_QUEUE),
            retryExchange = config.value("rabbitmq.retryExchange", ProductEventMessaging.RETRY_EXCHANGE),
            deadLetterExchange = config.value(
                "rabbitmq.deadLetterExchange",
                ProductEventMessaging.DEAD_LETTER_EXCHANGE
            ),
            deadLetterQueue = config.value("rabbitmq.deadLetterQueue", ProductEventMessaging.DEAD_LETTER_QUEUE),
            confirmTimeoutMillis = config.value("rabbitmq.confirmTimeoutMillis", "5000").toLong()
        )

        private fun ApplicationConfig.value(path: String, default: String): String =
            propertyOrNull(path)?.getString() ?: default
    }
}
