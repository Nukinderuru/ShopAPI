package com.nukinderuru.generator

import com.nukinderuru.events.ProductChangeEvent
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

interface RabbitPublisherChannel {
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

class RabbitMqProductEventPublisher(
    private val channel: RabbitPublisherChannel,
    private val exchange: String,
    private val routingKey: String,
    private val confirmTimeoutMillis: Long,
    private val json: Json = Json { encodeDefaults = true }
) : ProductEventPublisher {
    private val returned = AtomicBoolean(false)

    init {
        channel.confirmSelect()
        channel.addReturnListener { returned.set(true) }
    }

    override suspend fun publish(event: ProductChangeEvent) {
        returned.set(false)
        val properties = AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .type("product.changed")
            .build()
        val body = json.encodeToString(event).encodeToByteArray()

        channel.basicPublish(exchange, routingKey, properties, body, true)
        check(channel.waitForConfirms(confirmTimeoutMillis)) {
            "RabbitMQ did not confirm product change event ${event.eventId}"
        }
        check(!returned.get()) {
            "RabbitMQ returned unroutable product change event ${event.eventId}"
        }
    }
}

class RabbitPublisherChannelAdapter(private val channel: Channel) : RabbitPublisherChannel {
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