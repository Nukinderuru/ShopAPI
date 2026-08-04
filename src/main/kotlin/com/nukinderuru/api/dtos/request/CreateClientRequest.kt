package com.nukinderuru.api.dtos.request

import com.nukinderuru.common.serialization.LocalDateAsStringSerializer
import com.nukinderuru.data.db.dao.Gender
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class CreateClientRequest(
    val clientName: String,
    val clientSurname: String,
    @Serializable(with = LocalDateAsStringSerializer::class)
    val birthday: LocalDate,
    val gender: Gender,
    @Serializable(with = LocalDateAsStringSerializer::class)
    val registrationDate: LocalDate,
    val address: AddressRequest
)
