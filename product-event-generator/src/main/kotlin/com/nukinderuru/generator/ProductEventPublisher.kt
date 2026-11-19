package com.nukinderuru.generator

import com.nukinderuru.events.ProductChangeEvent

fun interface ProductEventPublisher {
    suspend fun publish(event: ProductChangeEvent)
}
