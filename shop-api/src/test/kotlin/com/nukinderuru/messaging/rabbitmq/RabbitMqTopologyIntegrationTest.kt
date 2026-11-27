package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.events.ProductEventMessaging
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.GetResponse
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import kotlin.io.use
import kotlin.test.Test
import kotlin.test.assertContentEquals

class RabbitMqTopologyIntegrationTest {
    @Test
    fun `topology routes confirmed messages through retry and dead letter paths`() {
        val rabbit = GenericContainer<Nothing>(DockerImageName.parse("rabbitmq:4-management-alpine"))
        rabbit.addExposedPort(5672)
        rabbit.addEnv("RABBITMQ_DEFAULT_USER", "test")
        rabbit.addEnv("RABBITMQ_DEFAULT_PASS", "test")
        rabbit.setWaitStrategy(Wait.forListeningPort())

        rabbit.start()
        try {
            val config = RabbitMqConfig(
                enabled = true,
                host = rabbit.host,
                port = rabbit.getMappedPort(5672),
                username = "test",
                password = "test",
                virtualHost = "/",
                exchange = "product.events.test",
                routingKey = "product.changed.test",
                mainQueue = "shop-api.product-events.test",
                retryExchange = "product.events.retry.test",
                deadLetterExchange = "product.events.dlx.test",
                deadLetterQueue = "shop-api.product-events.dlq.test",
                confirmTimeoutMillis = 5_000
            )
            config.createConnectionFactory().newConnection().use { connection ->
                connection.createChannel().use { channel ->
                    RabbitMqTopology(config).declare(channel)
                    channel.confirmSelect()

                    (1..ProductEventMessaging.MAX_RETRY_ATTEMPTS).forEach { attempt ->
                        channel.queueDeclarePassive("${config.mainQueue}.retry.$attempt")
                    }

                    val directBody = "direct".encodeToByteArray()
                    channel.basicPublish(
                        config.exchange,
                        config.routingKey,
                        true,
                        persistentProperties(),
                        directBody
                    )
                    check(channel.waitForConfirms(5_000))
                    val directDelivery = requireNotNull(channel.basicGet(config.mainQueue, false))
                    assertContentEquals(directBody, directDelivery.body)
                    channel.basicAck(directDelivery.envelope.deliveryTag, false)

                    (1..ProductEventMessaging.MAX_RETRY_ATTEMPTS).forEach { attempt ->
                        val retryBody = "retry-$attempt".encodeToByteArray()
                        channel.basicPublish(
                            config.retryExchange,
                            ProductEventMessaging.retryRoutingKey(attempt),
                            true,
                            persistentProperties(),
                            retryBody
                        )
                        check(channel.waitForConfirms(5_000))
                        val retryDelivery = requireNotNull(
                            channel.basicGet(ProductEventMessaging.retryQueue(config.mainQueue, attempt), false)
                        )
                        assertContentEquals(retryBody, retryDelivery.body)
                        channel.basicAck(retryDelivery.envelope.deliveryTag, false)
                    }


                    val retryBody = "retry".encodeToByteArray()
                    channel.basicPublish(
                        config.retryExchange,
                        ProductEventMessaging.retryRoutingKey(1),
                        true,
                        persistentProperties(),
                        retryBody
                    )
                    check(channel.waitForConfirms(5_000))
                    val retryDelivery = pollGet(channel, config.mainQueue, Duration.ofSeconds(10))
                    assertContentEquals(retryBody, retryDelivery.body)
                    channel.basicAck(retryDelivery.envelope.deliveryTag, false)

                    val deadBody = "dead".encodeToByteArray()
                    channel.basicPublish(
                        config.exchange,
                        config.routingKey,
                        true,
                        persistentProperties(),
                        deadBody
                    )
                    check(channel.waitForConfirms(5_000))
                    val rejected = requireNotNull(channel.basicGet(config.mainQueue, false))
                    channel.basicReject(rejected.envelope.deliveryTag, false)
                    val deadDelivery = pollGet(channel, config.deadLetterQueue, Duration.ofSeconds(5))
                    assertContentEquals(deadBody, deadDelivery.body)
                    channel.basicAck(deadDelivery.envelope.deliveryTag, false)
                }
            }
        } finally {
            rabbit.stop()
        }
    }

    private fun pollGet(
        channel: Channel,
        queue: String,
        timeout: Duration
    ): GetResponse {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            channel.basicGet(queue, false)?.let { return it }
            Thread.sleep(100)
        }
        error("No message arrived in $queue")
    }

    private fun persistentProperties() =
        AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .build()
}