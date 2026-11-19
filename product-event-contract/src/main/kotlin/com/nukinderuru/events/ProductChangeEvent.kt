package com.nukinderuru.events

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Serializable
data class ProductChangeEvent(
    @Serializable(with = EventUuidSerializer::class)
    val productId: UUID,
    @Serializable(with = EventBigDecimalSerializer::class)
    val oldPrice: BigDecimal,
    @Serializable(with = EventBigDecimalSerializer::class)
    val newPrice: BigDecimal,
    val oldAvailableStock: Int,
    val newAvailableStock: Int,
    @Serializable(with = EventInstantSerializer::class)
    val occurredAt: Instant = Instant.now()
)
