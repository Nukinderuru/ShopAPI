package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.events.ProductEventMessaging

class RabbitRetryPolicy(
    private val maxAttempts: Int = ProductEventMessaging.MAX_RETRY_ATTEMPTS
) {
    init {
        require(maxAttempts > 0)
    }

    fun attempt(headers: Map<String, Any?>): Int {
        val parsed = when (val value = headers[ProductEventMessaging.RETRY_HEADER]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
        return parsed?.takeIf { it in 0..maxAttempts } ?: 0
    }

    fun nextAttempt(currentAttempt: Int): Int? =
        currentAttempt.takeIf { it in 0 until maxAttempts }?.plus(1)
}
