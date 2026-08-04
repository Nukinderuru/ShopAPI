package com.nukinderuru.api.dtos.response

import com.nukinderuru.common.serialization.LocalDateAsStringSerializer
import com.nukinderuru.common.serialization.UuidAsStringSerializer
import com.nukinderuru.data.db.dao.Gender
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
data class ClientResponse(
    @Serializable(with = UuidAsStringSerializer::class)
    val id: UUID,
    val clientName: String,
    val clientSurname: String,
    @Serializable(with = LocalDateAsStringSerializer::class)
    val birthday: LocalDate,
    val gender: Gender,
    @Serializable(with = LocalDateAsStringSerializer::class)
    val registrationDate: LocalDate,
    val address: AddressResponse
)
