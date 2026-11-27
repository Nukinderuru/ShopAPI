package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.events.ProductEventMessaging
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import java.util.concurrent.atomic.AtomicBoolean

interface RabbitMessageChannel {
    fun confirmSelect()

    fun addReturnListener(listener: () -> Unit)

    fun basicPublish(
        exchange: String,
        routingKey: String,
        properties: AMQP.BasicProperties,
        body: ByteArray,
        mandatory: Boolean
    )

    fun waitForConfirms(timeoutMillis: Long): Boolean
}

class RabbitMessagePublisher(
    private val channel: RabbitMessageChannel,
    private val config: RabbitMqConfig
) {
    private val returned = AtomicBoolean(false)

    init {
        channel.confirmSelect()
        channel.addReturnListener { returned.set(true) }
    }

    fun publishRetry(body: ByteArray, headers: Map<String, Any>, attempt: Int) {
        require(attempt in 1..config.maxRetryAttempts)
        val retryHeaders = headers.toMutableMap().apply {
            this[ProductEventMessaging.RETRY_HEADER] = attempt
        }
        publish(
            exchange = config.retryExchange,
            routingKey = ProductEventMessaging.retryRoutingKey(attempt),
            body = body,
            headers = retryHeaders
        )
    }

    fun publishDeadLetter(
        body: ByteArray,
        headers: Map<String, Any>,
        errorType: String,
        errorMessage: String,
        attempt: Int
    ) {
        val deadLetterHeaders = headers.toMutableMap().apply {
            this[ProductEventMessaging.RETRY_HEADER] = attempt
            this[ProductEventMessaging.ERROR_TYPE_HEADER] = errorType
            this[ProductEventMessaging.ERROR_MESSAGE_HEADER] = errorMessage.take(1024)
        }
        publish(
            exchange = config.deadLetterExchange,
            routingKey = ProductEventMessaging.DEAD_LETTER_ROUTING_KEY,
            body = body,
            headers = deadLetterHeaders
        )
    }

    private fun publish(
        exchange: String,
        routingKey: String,
        body: ByteArray,
        headers: Map<String, Any>
    ) {
        returned.set(false)
        val properties = AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .headers(headers)
            .build()

        channel.basicPublish(exchange, routingKey, properties, body, true)
        check(channel.waitForConfirms(config.confirmTimeoutMillis)) {
            "RabbitMQ did not confirm message for $routingKey"
        }
        check(!returned.get()) {
            "RabbitMQ returned unroutable message for $routingKey"
        }
    }
}

class RabbitMessageChannelAdapter(private val channel: Channel) : RabbitMessageChannel {
    override fun confirmSelect() {
        channel.confirmSelect()
    }

    override fun addReturnListener(listener: () -> Unit) {
        channel.addReturnListener { _, _, _, _, _, _ -> listener() }
    }

    override fun basicPublish(
        exchange: String,
        routingKey: String,
        properties: AMQP.BasicProperties,
        body: ByteArray,
        mandatory: Boolean
    ) {
        channel.basicPublish(exchange, routingKey, mandatory, properties, body)
    }

    override fun waitForConfirms(timeoutMillis: Long): Boolean =
        channel.waitForConfirms(timeoutMillis)
}
