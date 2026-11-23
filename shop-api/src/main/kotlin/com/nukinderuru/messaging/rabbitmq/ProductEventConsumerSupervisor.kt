package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.domain.service.ProductEventService
import com.rabbitmq.client.DefaultConsumer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class ProductEventConsumerSupervisor(
    private val config: RabbitMqConfig,
    private val topology: RabbitMqTopology,
    private val eventService: ProductEventService
) : Closeable {
    private val closed = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var job: Job? = null
    private var activeResources: SessionResources? = null

    fun start() {
        if (!config.enabled || job != null) return
        job = scope.launch { supervise() }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        job?.cancel()
        closeSession()
        scope.cancel()
    }

    private suspend fun supervise() {
        while (scope.isActive && !closed.get()) {
            try {
                runSession()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                logger.warn("RabbitMQ consumer session failed; reconnecting", exception)
            } finally {
                closeSession()
            }
            if (scope.isActive && !closed.get()) delay(RECONNECT_DELAY_MILLIS.milliseconds)
        }
    }

    private suspend fun runSession() {
        val connection = config.createConnectionFactory().newConnection()
        val consumerChannel = connection.createChannel()
        val publisherChannel = connection.createChannel()
        val resources = SessionResources(connection, consumerChannel, publisherChannel)
        activeResources = resources
        topology.declare(consumerChannel)
        topology.declare(publisherChannel)
        consumerChannel.basicQos(1)

        val publisher = RabbitMessagePublisher(
            channel = RabbitMessageChannelAdapter(publisherChannel),
            config = config
        )
        val handler = ProductEventConsumer(eventService, publisher)
        val deliveries = Channel<RabbitDelivery>(capacity = 1)
        val consumer = object : DefaultConsumer(consumerChannel) {
            override fun handleDelivery(
                consumerTag: String,
                envelope: com.rabbitmq.client.Envelope,
                properties: com.rabbitmq.client.AMQP.BasicProperties,
                body: ByteArray
            ) {
                deliveries.trySend(RabbitDelivery(envelope, properties, body))
            }
        }
        consumerChannel.addShutdownListener { cause -> deliveries.close(cause) }
        consumerChannel.basicConsume(config.mainQueue, false, consumer)

        try {
            for (delivery in deliveries) {
                handler.handle(delivery, consumerChannel)
            }
        } finally {
            deliveries.close()
        }
    }

    private fun closeSession() {
        val resources = activeResources ?: return
        activeResources = null
        runCatching { resources.consumerChannel.close() }
        runCatching { resources.publisherChannel.close() }
        runCatching { resources.connection.close() }
    }

    private data class SessionResources(
        val connection: com.rabbitmq.client.Connection,
        val consumerChannel: com.rabbitmq.client.Channel,
        val publisherChannel: com.rabbitmq.client.Channel
    )

    private companion object {
        const val RECONNECT_DELAY_MILLIS = 5_000L
        val logger: Logger = LoggerFactory.getLogger(ProductEventConsumerSupervisor::class.java)
    }
}
