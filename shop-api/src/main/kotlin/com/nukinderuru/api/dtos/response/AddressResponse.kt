package com.nukinderuru.api.dtos.response

import com.nukinderuru.common.serialization.UuidAsStringSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AddressResponse(
    @Serializable(with = UuidAsStringSerializer::class)
    val id: UUID,
    val country: String,
    val city: String,
    val street: String
)
