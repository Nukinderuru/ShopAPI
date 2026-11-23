package com.nukinderuru.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class GeneratorConfigTest {
    @Test
    fun `fromEnvironment reads rabbitmq connection settings`() {
        val config = GeneratorConfig.fromEnvironment(
            mapOf(
                "RABBITMQ_HOST" to "rabbitmq",
                "RABBITMQ_PORT" to "5673",
                "RABBITMQ_USERNAME" to "publisher",
                "RABBITMQ_PASSWORD" to "secret",
                "RABBITMQ_VHOST" to "/events",
                "RABBITMQ_EXCHANGE" to "custom.events",
                "RABBITMQ_ROUTING_KEY" to "custom.changed",
                "RABBITMQ_CONFIRM_TIMEOUT_SECONDS" to "12",
            )
        )

        assertEquals("rabbitmq", config.rabbitMqHost)
        assertEquals(5673, config.rabbitMqPort)
        assertEquals("publisher", config.rabbitMqUsername)
        assertEquals("secret", config.rabbitMqPassword)
        assertEquals("/events", config.rabbitMqVirtualHost)
        assertEquals("custom.events", config.rabbitMqExchange)
        assertEquals("custom.changed", config.rabbitMqRoutingKey)
        assertEquals(12.seconds, config.rabbitMqConfirmTimeout)
    }
}