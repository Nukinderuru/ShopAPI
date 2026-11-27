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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class ProductEventConsumerSupervisor(
    private val config: RabbitMqConfig,
    private val topology: RabbitMqTopology,
    private val eventService: ProductEventService,
    private val connectionProvider: () -> com.rabbitmq.client.Connection = {
        config.createConnectionFactory().newConnection()
    },
    private val shutdownTimeoutMillis: Long = DEFAULT_SHUTDOWN_TIMEOUT_MILLIS
) : Closeable {
    private val closed = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    @Volatile
    private var job: Job? = null
    private val sessionLock = Any()
    private var activeResources: SessionResources? = null

    fun start() {
        if (!config.enabled || job != null) return
        job = scope.launch { supervise() }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return

        currentResources()?.stopAccepting()
        val runningJob = job
        if (runningJob != null) {
            runBlocking {
                withTimeoutOrNull(shutdownTimeoutMillis.milliseconds) {
                    runningJob.join()
                }
            }
            if (!runningJob.isCompleted) {
                runningJob.cancel()
            }
        }
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
        val connection = connectionProvider()
        var consumerChannel: com.rabbitmq.client.Channel? = null
        var publisherChannel: com.rabbitmq.client.Channel? = null
        var resourcesActivated = false

        try {
            val createdConsumerChannel = connection.createChannel()
            consumerChannel = createdConsumerChannel
            val createdPublisherChannel = connection.createChannel()
            publisherChannel = createdPublisherChannel
            val consumerChannel = createdConsumerChannel
            val publisherChannel = createdPublisherChannel
            val sessionResources = SessionResources(
                connection = connection,
                consumerChannel = consumerChannel,
                publisherChannel = publisherChannel,
                deliveries = Channel(capacity = 1)
            )
            if (!activateResources(sessionResources)) return
            resourcesActivated = true

            topology.declare(consumerChannel)
            topology.declare(publisherChannel)
            consumerChannel.basicQos(1)

            val publisher = RabbitMessagePublisher(
                channel = RabbitMessageChannelAdapter(publisherChannel),
                config = config
            )
            val handler = ProductEventConsumer(eventService, publisher)
            val consumer = object : DefaultConsumer(consumerChannel) {
                override fun handleDelivery(
                    consumerTag: String,
                    envelope: com.rabbitmq.client.Envelope,
                    properties: com.rabbitmq.client.AMQP.BasicProperties,
                    body: ByteArray
                ) {
                    sessionResources.deliveries.trySend(RabbitDelivery(envelope, properties, body))
                }
            }
            consumerChannel.addShutdownListener { cause -> sessionResources.deliveries.close(cause) }
            val consumerTag = consumerChannel.basicConsume(config.mainQueue, false, consumer)
            sessionResources.registerConsumerTag(consumerTag)

            for (delivery in sessionResources.deliveries) {
                handler.handle(delivery, consumerChannel)
            }
        } finally {
            if (resourcesActivated) {
                closeSession()
            } else {
                runCatching { publisherChannel?.close() }
                runCatching { consumerChannel?.close() }
                runCatching { connection.close() }
            }
        }
    }

    private fun activateResources(resources: SessionResources): Boolean = synchronized(sessionLock) {
        if (closed.get()) {
            false
        } else {
            activeResources = resources
            true
        }
    }

    private fun currentResources(): SessionResources? = synchronized(sessionLock) { activeResources }

    private fun closeSession() {
        val resources = synchronized(sessionLock) {
            activeResources.also { activeResources = null }
        } ?: return
        resources.stopAccepting()
        runCatching { resources.consumerChannel.close() }
        runCatching { resources.publisherChannel.close() }
        runCatching { resources.connection.close() }
    }

    private data class SessionResources(
        val connection: com.rabbitmq.client.Connection,
        val consumerChannel: com.rabbitmq.client.Channel,
        val publisherChannel: com.rabbitmq.client.Channel,
        val deliveries: Channel<RabbitDelivery>
    ) {
        private val shutdownRequested = AtomicBoolean(false)

        @Volatile
        private var consumerTag: String? = null

        fun registerConsumerTag(tag: String) {
            consumerTag = tag
            if (shutdownRequested.get()) {
                cancelConsumer(tag)
            }
        }

        fun stopAccepting() {
            if (!shutdownRequested.compareAndSet(false, true)) return
            consumerTag?.let { tag -> cancelConsumer(tag) }
            deliveries.close()
        }

        private fun cancelConsumer(tag: String) {
            runCatching { consumerChannel.basicCancel(tag) }
                .onFailure { exception -> logger.warn("Failed to cancel RabbitMQ consumer $tag", exception) }
        }
    }

    private companion object {
        const val RECONNECT_DELAY_MILLIS = 5_000L
        const val DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 10_000L
        val logger: Logger = LoggerFactory.getLogger(ProductEventConsumerSupervisor::class.java)
    }
}
