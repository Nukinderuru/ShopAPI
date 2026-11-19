package com.nukinderuru.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class GeneratorConfigTest {
    @Test
    fun `fromEnvironment reads gateway endpoint and timeout`() {
        val config = GeneratorConfig.fromEnvironment(
            mapOf(
                "PRODUCT_EVENT_GATEWAY_URL" to "http://app:8080/custom-events",
                "PRODUCT_EVENT_HTTP_TIMEOUT_SECONDS" to "12"
            )
        )

        assertEquals("http://app:8080/custom-events", config.gatewayUrl)
        assertEquals(12.seconds, config.httpTimeout)
    }
}