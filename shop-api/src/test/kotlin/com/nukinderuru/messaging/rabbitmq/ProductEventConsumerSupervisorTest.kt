package com.nukinderuru.messaging.rabbitmq

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.domain.service.ProductEventProcessingResult
import com.nukinderuru.domain.service.ProductEventService
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ProductEventConsumerSupervisorTest {
    @Test
    fun `close cancels consumer and waits for current delivery before closing resources`() {
        val connection = mockk<Connection>(relaxed = true)
        val consumerChannel = mockk<Channel>(relaxed = true)
        val publisherChannel = mockk<Channel>(relaxed = true)
        val consumerSlot = CapturingSlot<DefaultConsumer>()
        val processingCompleted = AtomicBoolean(false)
        val service = mockk<ProductEventService>()

        every { connection.createChannel() } returnsMany listOf(consumerChannel, publisherChannel)
        every { consumerChannel.basicConsume(any(), false, capture(consumerSlot)) } returns "consumer-tag"
        coEvery { service.process(any()) } coAnswers {
            delay(100.milliseconds)
            processingCompleted.set(true)
            ProductEventProcessingResult.Applied(productResponse())
        }

        val supervisor = supervisor(
            connectionProvider = { connection },
            eventService = service
        )
        supervisor.start()

        verify(timeout = 2_000) { consumerChannel.basicConsume(any(), false, capture(consumerSlot)) }
        consumerSlot.captured.handleDelivery(
            "consumer-tag",
            Envelope(1L, false, "product.events", "product.changed"),
            AMQP.BasicProperties.Builder().build(),
            eventJson()
        )

        coVerify(timeout = 2_000) { service.process(any()) }
        supervisor.close()

        assertTrue(processingCompleted.get())
        verify(timeout = 1_000, exactly = 1) { consumerChannel.basicCancel("consumer-tag") }
        verify(exactly = 1) { consumerChannel.close() }
        verify(exactly = 1) { publisherChannel.close() }
        verify(exactly = 1) { connection.close() }
    }

    @Test
    fun `close cancels processing after shutdown timeout and closes resources`() {
        val connection = mockk<Connection>(relaxed = true)
        val consumerChannel = mockk<Channel>(relaxed = true)
        val publisherChannel = mockk<Channel>(relaxed = true)
        val consumerSlot = CapturingSlot<DefaultConsumer>()
        val service = mockk<ProductEventService>()

        every { connection.createChannel() } returnsMany listOf(consumerChannel, publisherChannel)
        every { consumerChannel.basicConsume(any(), false, capture(consumerSlot)) } returns "consumer-tag"
        coEvery { service.process(any()) } coAnswers {
            delay(10_000.milliseconds)
            ProductEventProcessingResult.Applied(productResponse())
        }

        val supervisor = supervisor(
            connectionProvider = { connection },
            eventService = service,
            shutdownTimeoutMillis = 50
        )
        supervisor.start()
        verify(timeout = 2_000) { consumerChannel.basicConsume(any(), false, capture(consumerSlot)) }
        consumerSlot.captured.handleDelivery(
            "consumer-tag",
            Envelope(1L, false, "product.events", "product.changed"),
            AMQP.BasicProperties.Builder().build(),
            eventJson()
        )
        coVerify(timeout = 2_000) { service.process(any()) }

        supervisor.close()

        verify(timeout = 1_000, exactly = 1) { consumerChannel.basicCancel("consumer-tag") }
        verify(exactly = 1) { consumerChannel.close() }
        verify(exactly = 1) { publisherChannel.close() }
        verify(exactly = 1) { connection.close() }
    }

    @Test
    fun `close logs failed consumer cancellation and closes all resources`() {
        val connection = mockk<Connection>(relaxed = true)
        val consumerChannel = mockk<Channel>(relaxed = true)
        val publisherChannel = mockk<Channel>(relaxed = true)
        val consumerSlot = CapturingSlot<DefaultConsumer>()
        val service = mockk<ProductEventService>()
        val logger = LoggerFactory.getLogger(ProductEventConsumerSupervisor::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }

        every { connection.createChannel() } returnsMany listOf(consumerChannel, publisherChannel)
        every { consumerChannel.basicConsume(any(), false, capture(consumerSlot)) } returns "consumer-tag"
        every { consumerChannel.basicCancel("consumer-tag") } throws IOException("cancel failed")
        logger.addAppender(appender)

        try {
            val supervisor = supervisor(
                connectionProvider = { connection },
                eventService = service
            )
            supervisor.start()

            verify(timeout = 2_000) { consumerChannel.basicConsume(any(), false, capture(consumerSlot)) }
            supervisor.close()

            val warning = appender.list.single { it.level == Level.WARN }
            assertTrue(warning.formattedMessage.contains("consumer-tag"))
            assertTrue(warning.throwableProxy?.className == IOException::class.java.name)
            verify(exactly = 1) { consumerChannel.close() }
            verify(exactly = 1) { publisherChannel.close() }
            verify(exactly = 1) { connection.close() }
        } finally {
            logger.detachAppender(appender)
        }
    }

    @Test
    fun `partial session initialization closes connection and created channel`() {
        val connection = mockk<Connection>(relaxed = true)
        val consumerChannel = mockk<Channel>(relaxed = true)
        val service = mockk<ProductEventService>()

        every { connection.createChannel() } returns consumerChannel andThenThrows IllegalStateException("publisher channel")

        val supervisor = supervisor(
            connectionProvider = { connection },
            eventService = service,
            shutdownTimeoutMillis = 50
        )
        supervisor.start()
        verify(timeout = 2_000) { connection.createChannel() }

        supervisor.close()

        verify(exactly = 1) { consumerChannel.close() }
        verify(exactly = 1) { connection.close() }
    }

    private fun supervisor(
        connectionProvider: () -> Connection,
        eventService: ProductEventService,
        shutdownTimeoutMillis: Long = 1_000
    ) = ProductEventConsumerSupervisor(
        config = config(),
        topology = mockk(relaxed = true),
        eventService = eventService,
        connectionProvider = connectionProvider,
        shutdownTimeoutMillis = shutdownTimeoutMillis
    )

    private fun eventJson(): ByteArray = """
        {
          "eventId":"11111111-1111-1111-1111-111111111111",
          "productId":"22222222-2222-2222-2222-222222222222",
          "oldPrice":"10.00",
          "newPrice":"11.00",
          "oldAvailableStock":5,
          "newAvailableStock":4,
          "occurredAt":"2026-11-20T12:00:00Z"
        }
    """.trimIndent().encodeToByteArray()

    private fun productResponse() = ProductResponse(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        name = "Toaster",
        category = "Kitchen",
        price = BigDecimal("11.00"),
        availableStock = 4,
        lastUpdateDate = LocalDate.of(2026, 11, 20),
        supplierId = null,
        imageId = null
    )

    private fun config() = RabbitMqConfig(
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
}