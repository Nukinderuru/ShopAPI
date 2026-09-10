package com.nukinderuru.auth

data class RegisterHttpRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val password: String
)

data class AuthHttpRequest(
    val email: String,
    val password: String
)

data class ResetPasswordHttpRequest(
    val email: String
)

data class TokenHttpResponse(
    val token: String
)

data class ResetPasswordHttpResponse(
    val success: Boolean
)
