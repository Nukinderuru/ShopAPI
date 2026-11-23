package com.nukinderuru.messaging.rabbitmq

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RabbitRetryPolicyTest {
    private val policy = RabbitRetryPolicy(maxAttempts = 3)

    @Test
    fun `missing header means original delivery`() {
        assertEquals(0, policy.attempt(emptyMap()))
    }

    @Test
    fun `retry attempt increments until maximum`() {
        assertEquals(1, policy.nextAttempt(0))
        assertEquals(2, policy.nextAttempt(1))
        assertEquals(3, policy.nextAttempt(2))
        assertNull(policy.nextAttempt(3))
    }

    @Test
    fun `invalid header is treated as original delivery`() {
        assertEquals(0, policy.attempt(mapOf("x-retry-attempt" to "not-a-number")))
    }
}