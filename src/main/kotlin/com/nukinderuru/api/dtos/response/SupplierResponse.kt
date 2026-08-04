package com.nukinderuru.api.dtos.response

import com.nukinderuru.common.serialization.UuidAsStringSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SupplierResponse(
    @Serializable(with = UuidAsStringSerializer::class)
    val id: UUID,
    val name: String,
    val address: AddressResponse,
    val phoneNumber: String
)
