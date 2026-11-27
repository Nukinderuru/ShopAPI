package com.nukinderuru.events

object ProductEventMessaging {
    const val EXCHANGE = "product.events"
    const val ROUTING_KEY = "product.changed"
    const val MAIN_QUEUE = "shop-api.product-events"
    const val RETRY_EXCHANGE = "product.events.retry"
    const val DEAD_LETTER_EXCHANGE = "product.events.dlx"
    const val DEAD_LETTER_ROUTING_KEY = "product.dead"
    const val DEAD_LETTER_QUEUE = "shop-api.product-events.dlq"
    const val RETRY_HEADER = "x-retry-attempt"
    const val ERROR_TYPE_HEADER = "x-error-type"
    const val ERROR_MESSAGE_HEADER = "x-error-message"
    const val MAX_RETRY_ATTEMPTS = 3
    val RETRY_DELAYS_MILLISECONDS = listOf(5_000L, 30_000L, 120_000L)

    fun retryQueue(mainQueue: String, attempt: Int): String = "$mainQueue.retry.$attempt"

    fun retryRoutingKey(attempt: Int): String = "product.retry.$attempt"
}