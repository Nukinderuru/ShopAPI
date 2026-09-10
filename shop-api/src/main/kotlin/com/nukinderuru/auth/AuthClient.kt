package com.nukinderuru.auth

interface AuthClient {
    suspend fun register(request: RegisterHttpRequest): String
    suspend fun authenticate(request: AuthHttpRequest): String
    suspend fun resetPassword(request: ResetPasswordHttpRequest): Boolean
    suspend fun validateToken(token: String): Boolean
}
