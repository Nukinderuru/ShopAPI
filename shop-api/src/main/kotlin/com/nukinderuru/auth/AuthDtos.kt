package com.nukinderuru.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterHttpRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val password: String
)

@Serializable
data class AuthHttpRequest(
    val email: String,
    val password: String
)

@Serializable
data class ResetPasswordHttpRequest(
    val email: String
)

@Serializable
data class TokenHttpResponse(
    val token: String
)

@Serializable
data class ResetPasswordHttpResponse(
    val success: Boolean
)
