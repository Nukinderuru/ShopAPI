package com.nukinderuru.api.dtos.request

import kotlinx.serialization.Serializable

@Serializable
data class AddressRequest(
    val country: String,
    val city: String,
    val street: String
)
