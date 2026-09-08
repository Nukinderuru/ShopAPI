package com.nukinderuru.common.mapper

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateClientRequest
import com.nukinderuru.api.dtos.response.AddressResponse
import com.nukinderuru.api.dtos.response.ClientResponse
import com.nukinderuru.data.db.dao.AddressEntity
import com.nukinderuru.data.db.dao.ClientEntity
import com.nukinderuru.data.db.dao.Gender
import java.time.LocalDate

data class AddressDraft(
    val country: String,
    val city: String,
    val street: String,
)

data class ClientDraft(
    val clientName: String,
    val clientSurname: String,
    val birthday: LocalDate,
    val gender: Gender,
    val registrationDate: LocalDate,
)

fun AddressRequest.toAddressDraft(): AddressDraft = AddressDraft(
    country = country.trim(),
    city = city.trim(),
    street = street.trim(),
)

fun CreateClientRequest.toClientDraft(): ClientDraft = ClientDraft(
    clientName = clientName.trim(),
    clientSurname = clientSurname.trim(),
    birthday = birthday,
    gender = gender,
    registrationDate = registrationDate,
)

fun AddressEntity.updateFrom(addressDraft: AddressDraft) {
    country = addressDraft.country
    city = addressDraft.city
    street = addressDraft.street
}

fun AddressEntity.toResponse(): AddressResponse = AddressResponse(
    id = id.value,
    country = country,
    city = city,
    street = street,
)

fun ClientEntity.toResponse(): ClientResponse = ClientResponse(
    id = id.value,
    clientName = clientName,
    clientSurname = clientSurname,
    birthday = birthday,
    gender = gender,
    registrationDate = registrationDate,
    address = address.toResponse(),
)
