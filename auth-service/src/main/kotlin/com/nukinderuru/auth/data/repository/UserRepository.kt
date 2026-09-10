package com.nukinderuru.auth.data.repository

import java.util.UUID

data class NewUser(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val passwordHash: String,
    val passwordSalt: String
)

data class AuthUser(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val passwordHash: String,
    val passwordSalt: String
)

interface UserRepository {
    suspend fun create(user: NewUser): AuthUser
    suspend fun findByEmail(email: String): AuthUser?
    suspend fun findById(id: UUID): AuthUser?
    suspend fun updatePassword(userId: UUID, passwordHash: String, passwordSalt: String): Boolean
}

class DuplicateUserException(message: String) : RuntimeException(message)
