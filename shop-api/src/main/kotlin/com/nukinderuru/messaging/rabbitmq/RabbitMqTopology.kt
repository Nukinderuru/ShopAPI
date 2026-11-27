package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.events.ProductEventMessaging
import com.rabbitmq.client.Channel

class RabbitMqTopology(private val config: RabbitMqConfig) {
    fun declare(channel: Channel) {
        channel.exchangeDeclare(config.exchange, "direct", true)
        channel.exchangeDeclare(config.retryExchange, "direct", true)
        channel.exchangeDeclare(config.deadLetterExchange, "direct", true)

        channel.queueDeclare(
            config.mainQueue,
            true,
            false,
            false,
            mapOf(
                "x-dead-letter-exchange" to config.deadLetterExchange,
                "x-dead-letter-routing-key" to ProductEventMessaging.DEAD_LETTER_ROUTING_KEY
            )
        )
        channel.queueBind(config.mainQueue, config.exchange, config.routingKey)

        channel.queueDeclare(config.deadLetterQueue, true, false, false, emptyMap())
        channel.queueBind(
            config.deadLetterQueue,
            config.deadLetterExchange,
            ProductEventMessaging.DEAD_LETTER_ROUTING_KEY
        )

        ProductEventMessaging.RETRY_DELAYS_MILLISECONDS.forEachIndexed { index, delayMillis ->
            val attempt = index + 1
            val retryQueue = ProductEventMessaging.retryQueue(config.mainQueue, attempt)
            channel.queueDeclare(
                retryQueue,
                true,
                false,
                false,
                mapOf(
                    "x-message-ttl" to delayMillis,
                    "x-dead-letter-exchange" to config.exchange,
                    "x-dead-letter-routing-key" to config.routingKey
                )
            )
            channel.queueBind(
                retryQueue,
                config.retryExchange,
                ProductEventMessaging.retryRoutingKey(attempt)
            )
        }
    }
}
