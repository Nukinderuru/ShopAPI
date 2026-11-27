package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.domain.service.ProductEventService
import com.rabbitmq.client.Channel
import io.mockk.mockk
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import kotlin.io.use
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductEventConsumerSupervisorIntegrationTest {
    @Test
    fun `supervisor can be started and stopped repeatedly`() {
        val rabbit = GenericContainer<Nothing>(DockerImageName.parse("rabbitmq:4-management-alpine")).apply {
            addExposedPort(5672)
            addEnv("RABBITMQ_DEFAULT_USER", "test")
            addEnv("RABBITMQ_DEFAULT_PASS", "test")
            setWaitStrategy(Wait.forListeningPort())
        }

        rabbit.start()
        try {
            val probeConfig = config(rabbit, "probe")
            probeConfig.createConnectionFactory().newConnection().use { probeConnection ->
                repeat(10) { iteration ->
                    val config = config(rabbit, iteration.toString())
                    val topology = RabbitMqTopology(config)
                    probeConnection.createChannel().use { setupChannel ->
                        topology.declare(setupChannel)
                    }
                    probeConnection.createChannel().use { probeChannel ->
                        val supervisor = ProductEventConsumerSupervisor(
                            config = config,
                            topology = topology,
                            eventService = mockk<ProductEventService>(relaxed = true)
                        )

                        try {
                            supervisor.start()
                            awaitConsumerCount(probeChannel, config.mainQueue, 1)
                        } finally {
                            supervisor.close()
                        }

                        awaitConsumerCount(probeChannel, config.mainQueue, 0)
                    }
                }
            }
        } finally {
            rabbit.stop()
        }
    }

    private fun awaitConsumerCount(channel: Channel, queue: String, expected: Int) {
        val deadline = System.nanoTime() + POLL_TIMEOUT.toNanos()
        var actual: Int? = null
        while (System.nanoTime() < deadline) {
            actual = channel.queueDeclarePassive(queue).consumerCount
            if (actual == expected) return
            Thread.sleep(POLL_INTERVAL.toMillis())
        }
        assertEquals(expected, actual, "Unexpected consumer count for $queue")
    }

    private fun config(rabbit: GenericContainer<Nothing>, suffix: String) = RabbitMqConfig(
        enabled = true,
        host = rabbit.host,
        port = rabbit.getMappedPort(5672),
        username = "test",
        password = "test",
        virtualHost = "/",
        exchange = "product.events.lifecycle.$suffix",
        routingKey = "product.changed.lifecycle.$suffix",
        mainQueue = "shop-api.product-events.lifecycle.$suffix",
        retryExchange = "product.events.retry.lifecycle.$suffix",
        deadLetterExchange = "product.events.dlx.lifecycle.$suffix",
        deadLetterQueue = "shop-api.product-events.dlq.lifecycle.$suffix",
        confirmTimeoutMillis = 5_000
    )

    private companion object {
        val POLL_TIMEOUT: Duration = Duration.ofSeconds(10)
        val POLL_INTERVAL: Duration = Duration.ofMillis(50)
    }
}