package com.nukinderuru.generator

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ProductChangeEvent(
    val productId: UUID,
    val oldPrice: BigDecimal,
    val newPrice: BigDecimal,
    val oldAvailableStock: Int,
    val newAvailableStock: Int,
    val occurredAt: Instant = Instant.now()
)
