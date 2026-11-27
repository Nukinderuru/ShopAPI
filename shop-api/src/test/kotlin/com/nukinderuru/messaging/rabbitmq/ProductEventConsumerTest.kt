package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.domain.service.ProductEventProcessingResult
import com.nukinderuru.domain.service.ProductEventService
import com.nukinderuru.events.ProductChangeEvent
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import java.math.BigDecimal
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ProductEventConsumerTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `applied event is acknowledged without retry`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        coEvery { service.process(any()) } returns ProductEventProcessingResult.Applied(productResponse())

        ProductEventConsumer(service, publisher()).handle(delivery(), channel)

        verify(exactly = 1) { channel.basicAck(1L, false) }
        verify(exactly = 0) { channel.basicReject(any(), any()) }
    }


    @Test
    fun `stale event is acknowledged without retry`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        val publisher = publisher()
        coEvery { service.process(any()) } returns ProductEventProcessingResult.Stale

        ProductEventConsumer(service, publisher).handle(delivery(), channel)

        verify(exactly = 1) { channel.basicAck(1L, false) }
        verify(exactly = 0) { channel.basicReject(any(), any()) }
    }

    @Test
    fun `duplicate event is acknowledged without retry`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        coEvery { service.process(any()) } returns ProductEventProcessingResult.Duplicate

        ProductEventConsumer(service, publisher()).handle(delivery(), channel)

        verify(exactly = 1) { channel.basicAck(1L, false) }
        verify(exactly = 0) { channel.basicReject(any(), any()) }
    }

    @Test
    fun `missing product event is acknowledged without retry`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        coEvery { service.process(any()) } returns ProductEventProcessingResult.NotFound

        ProductEventConsumer(service, publisher()).handle(delivery(), channel)

        verify(exactly = 1) { channel.basicAck(1L, false) }
        verify(exactly = 0) { channel.basicReject(any(), any()) }
    }

    @Test
    fun `acknowledgement failure is propagated without retry or dead letter publication`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        val messagePublisher = RecordingMessagePublisher()
        val acknowledgementFailure = IOException("acknowledgement failed")
        coEvery { service.process(any()) } returns ProductEventProcessingResult.Applied(productResponse())
        io.mockk.every { channel.basicAck(1L, false) } throws acknowledgementFailure

        assertFailsWith<IOException> {
            ProductEventConsumer(service, messagePublisher.publisher()).handle(delivery(), channel)
        }

        kotlin.test.assertEquals(emptyList(), messagePublisher.retryAttempts)
        kotlin.test.assertEquals(emptyList(), messagePublisher.deadLetterAttempts)
        verify(exactly = 1) { channel.basicAck(1L, false) }
    }

    @Test
    fun `invalid json is rejected without requeue`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)

        ProductEventConsumer(service, publisher()).handle(delivery(body = "not-json".encodeToByteArray()), channel)

        verify(exactly = 1) { channel.basicReject(1L, false) }
        verify(exactly = 0) { channel.basicAck(any(), any()) }
    }

    @Test
    fun `business validation is rejected without requeue`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        coEvery { service.process(any()) } throws ValidationException("invalid event")

        ProductEventConsumer(service, publisher()).handle(delivery(), channel)

        verify(exactly = 1) { channel.basicReject(1L, false) }
        verify(exactly = 0) { channel.basicAck(any(), any()) }
    }

    @Test
    fun `transient error publishes next retry and acknowledges original`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        val messagePublisher = RecordingMessagePublisher()
        coEvery { service.process(any()) } throws SQLException("temporary", "08006")

        ProductEventConsumer(service, messagePublisher.publisher()).handle(delivery(), channel)

        kotlin.test.assertEquals(1, messagePublisher.retryAttempts.single())
        verify(exactly = 1) { channel.basicAck(1L, false) }
    }

    @Test
    fun `successful retry publication followed by acknowledgement failure requires channel restart without republishing`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        val messagePublisher = RecordingMessagePublisher()
        coEvery { service.process(any()) } throws SQLException("temporary", "08006")
        io.mockk.every { channel.basicAck(1L, false) } throws IOException("acknowledgement failed")

        assertFailsWith<ConsumerChannelRestartRequired> {
            ProductEventConsumer(service, messagePublisher.publisher()).handle(delivery(), channel)
        }

        kotlin.test.assertEquals(listOf(1), messagePublisher.retryAttempts)
        kotlin.test.assertEquals(emptyList(), messagePublisher.deadLetterAttempts)
        verify(exactly = 1) { channel.basicAck(1L, false) }
    }

    @Test
    fun `failed retry publication closes session without acknowledging original`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        val messagePublisher = RecordingMessagePublisher(confirmResult = false)
        coEvery { service.process(any()) } throws SQLException("temporary", "08006")

        assertFailsWith<ConsumerChannelRestartRequired> {
            ProductEventConsumer(service, messagePublisher.publisher()).handle(delivery(), channel)
        }

        verify(exactly = 0) { channel.basicAck(any(), any()) }
    }

    @Test
    fun `exhausted retry is published to dead letter queue and acknowledged`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        val messagePublisher = RecordingMessagePublisher()
        coEvery { service.process(any()) } throws SQLException("temporary", "08006")

        ProductEventConsumer(service, messagePublisher.publisher()).handle(
            delivery(headers = mapOf("x-retry-attempt" to 3)),
            channel
        )

        kotlin.test.assertEquals(3, messagePublisher.deadLetterAttempts.single())
        verify(exactly = 1) { channel.basicAck(1L, false) }
    }

    @Test
    fun `successful dead letter publication followed by acknowledgement failure requires channel restart without republishing`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        val messagePublisher = RecordingMessagePublisher()
        coEvery { service.process(any()) } throws SQLException("temporary", "08006")
        io.mockk.every { channel.basicAck(1L, false) } throws IOException("acknowledgement failed")

        assertFailsWith<ConsumerChannelRestartRequired> {
            ProductEventConsumer(service, messagePublisher.publisher()).handle(
                delivery(headers = mapOf("x-retry-attempt" to 3)),
                channel
            )
        }

        kotlin.test.assertEquals(emptyList(), messagePublisher.retryAttempts)
        kotlin.test.assertEquals(listOf(3), messagePublisher.deadLetterAttempts)
        verify(exactly = 1) { channel.basicAck(1L, false) }
    }

    @Test
    fun `failed dead letter publication closes session without acknowledging original`() = runTest {
        val service = mockk<ProductEventService>()
        val channel = mockk<Channel>(relaxed = true)
        val messagePublisher = RecordingMessagePublisher(confirmResult = false)
        coEvery { service.process(any()) } throws SQLException("temporary", "08006")

        assertFailsWith<ConsumerChannelRestartRequired> {
            ProductEventConsumer(service, messagePublisher.publisher()).handle(
                delivery(headers = mapOf("x-retry-attempt" to 3)),
                channel
            )
        }

        verify(exactly = 0) { channel.basicAck(any(), any()) }
    }

    private fun delivery(
        body: ByteArray = json.encodeToString(event()).encodeToByteArray(),
        headers: Map<String, Any> = emptyMap()
    ) = RabbitDelivery(
        envelope = Envelope(1L, false, "product.events", "product.changed"),
        properties = AMQP.BasicProperties.Builder().headers(headers).build(),
        body = body
    )

    private fun event() = ProductChangeEvent(
        eventId = EVENT_ID,
        productId = PRODUCT_ID,
        oldPrice = BigDecimal("10.00"),
        newPrice = BigDecimal("11.00"),
        oldAvailableStock = 5,
        newAvailableStock = 4,
        occurredAt = Instant.parse("2026-11-20T12:00:00Z")
    )

    private fun publisher(): RabbitMessagePublisher = RecordingMessagePublisher().publisher()

    private fun productResponse() = ProductResponse(
        id = PRODUCT_ID,
        name = "Toaster",
        category = "Kitchen",
        price = BigDecimal("11.00"),
        availableStock = 4,
        lastUpdateDate = LocalDate.of(2026, 11, 20),
        supplierId = null,
        imageId = null
    )

    private class RecordingMessagePublisher(
        private val confirmResult: Boolean = true
    ) {
        val retryAttempts = mutableListOf<Int>()
        val deadLetterAttempts = mutableListOf<Int>()

        fun publisher(): RabbitMessagePublisher = RabbitMessagePublisher(
            channel = object : RabbitMessageChannel {
                private var returnListener: (() -> Unit)? = null

                override fun confirmSelect() = Unit

                override fun addReturnListener(listener: () -> Unit) {
                    returnListener = listener
                }

                override fun basicPublish(
                    exchange: String,
                    routingKey: String,
                    properties: AMQP.BasicProperties,
                    body: ByteArray,
                    mandatory: Boolean
                ) {
                    properties.headers?.get("x-retry-attempt")?.let {
                        val attempt = (it as Number).toInt()
                        if (exchange == "product.events.dlx") {
                            deadLetterAttempts += attempt
                        } else {
                            retryAttempts += attempt
                        }
                    }
                }

                override fun waitForConfirms(timeoutMillis: Long): Boolean = confirmResult
            },
            config = RabbitMqConfig(
                enabled = true,
                host = "localhost",
                port = 5672,
                username = "guest",
                password = "guest",
                virtualHost = "/",
                exchange = "product.events",
                routingKey = "product.changed",
                mainQueue = "shop-api.product-events",
                retryExchange = "product.events.retry",
                deadLetterExchange = "product.events.dlx",
                deadLetterQueue = "shop-api.product-events.dlq",
                confirmTimeoutMillis = 1000
            )
        )
    }

    private companion object {
        val EVENT_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val PRODUCT_ID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    }
}