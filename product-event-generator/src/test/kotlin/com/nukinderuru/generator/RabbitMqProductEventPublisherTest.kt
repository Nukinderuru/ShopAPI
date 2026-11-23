package com.nukinderuru.generator

import com.nukinderuru.events.ProductChangeEvent
import com.rabbitmq.client.AMQP
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RabbitMqProductEventPublisherTest {
    @Test
    fun `publish sends persistent json and waits for confirm`() = runTest {
        val channel = RecordingPublisherChannel()
        val publisher = RabbitMqProductEventPublisher(
            channel = channel,
            exchange = "product.events",
            routingKey = "product.changed",
            confirmTimeoutMillis = 1000
        )

        publisher.publish(event())

        assertEquals("product.events", channel.exchange)
        assertEquals("product.changed", channel.routingKey)
        assertTrue(channel.mandatory)
        assertEquals(2, channel.properties.deliveryMode)
        assertEquals("application/json", channel.properties.contentType)
        assertTrue(channel.body.decodeToString().contains("\"eventId\":\"$EVENT_ID\""))
        assertTrue(channel.confirmSelected)
        assertTrue(channel.confirmWaited)
    }

    @Test
    fun `publish fails when broker does not confirm`() = runTest {
        val channel = RecordingPublisherChannel(confirmResult = false)
        val publisher = RabbitMqProductEventPublisher(
            channel = channel,
            exchange = "product.events",
            routingKey = "product.changed",
            confirmTimeoutMillis = 1000
        )

        assertFailsWith<IllegalStateException> { publisher.publish(event()) }
    }

    @Test
    fun `publish fails when mandatory message is returned`() = runTest {
        val channel = RecordingPublisherChannel(shouldReturn = true)
        val publisher = RabbitMqProductEventPublisher(
            channel = channel,
            exchange = "product.events",
            routingKey = "product.changed",
            confirmTimeoutMillis = 1000
        )

        assertFailsWith<IllegalStateException> { publisher.publish(event()) }
    }

    private fun event() = ProductChangeEvent(
        eventId = EVENT_ID,
        productId = PRODUCT_ID,
        oldPrice = BigDecimal("100.00"),
        newPrice = BigDecimal("115.00"),
        oldAvailableStock = 10,
        newAvailableStock = 7,
        occurredAt = Instant.parse("2026-11-20T12:00:00Z")
    )

    private class RecordingPublisherChannel(
        private val confirmResult: Boolean = true,
        shouldReturn: Boolean = false
    ) : RabbitPublisherChannel {
        private val willReturn = shouldReturn
        var exchange: String? = null
        var routingKey: String? = null
        var properties: AMQP.BasicProperties = AMQP.BasicProperties()
        var body: ByteArray = ByteArray(0)
        var mandatory = false
        var confirmSelected = false
        var confirmWaited = false

        private var returnListener: (() -> Unit)? = if (shouldReturn) ({}) else null

        override fun confirmSelect() {
            confirmSelected = true
        }

        override fun addReturnListener(listener: () -> Unit) {
            returnListener = listener
            if (willReturn) listener()
        }

        override fun basicPublish(
            exchange: String,
            routingKey: String,
            properties: AMQP.BasicProperties,
            body: ByteArray,
            mandatory: Boolean
        ) {
            this.exchange = exchange
            this.routingKey = routingKey
            this.properties = properties
            this.body = body
            this.mandatory = mandatory
            if (willReturn) returnListener?.invoke()
        }

        override fun waitForConfirms(timeoutMillis: Long): Boolean {
            confirmWaited = true
            return confirmResult
        }
    }

    private companion object {
        val EVENT_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val PRODUCT_ID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    }
}