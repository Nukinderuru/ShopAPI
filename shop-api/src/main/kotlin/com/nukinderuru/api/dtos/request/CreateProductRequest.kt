package com.nukinderuru.api.dtos.request

import com.nukinderuru.common.serialization.BigDecimalAsStringSerializer
import com.nukinderuru.common.serialization.LocalDateAsStringSerializer
import com.nukinderuru.common.serialization.UuidAsStringSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Serializable
data class CreateProductRequest(
    val name: String,
    val category: String,
    @Serializable(with = BigDecimalAsStringSerializer::class)
    val price: BigDecimal,
    val availableStock: Int,
    @Serializable(with = LocalDateAsStringSerializer::class)
    val lastUpdateDate: LocalDate,
    @Serializable(with = UuidAsStringSerializer::class)
    val supplierId: UUID? = null,
    @Serializable(with = UuidAsStringSerializer::class)
    val imageId: UUID? = null
)
