package com.nukinderuru.api.dtos.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateSupplierRequest(
    val name: String,
    val address: AddressRequest,
    val phoneNumber: String
)
