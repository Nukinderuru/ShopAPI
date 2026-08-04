package com.nukinderuru.api.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val code: Int,
    val description: String
)
