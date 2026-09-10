package com.nukinderuru.auth.data.repository

import com.nukinderuru.auth.data.dao.UserEntity
import com.nukinderuru.auth.data.dao.UserTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.sql.SQLIntegrityConstraintViolationException
import java.time.Instant
import java.util.UUID

class ExposedUserRepository(private val database: Database) : UserRepository {
    override suspend fun create(user: NewUser): AuthUser = dbQuery {
        try {
            UserEntity.new {
                email = user.email
                firstName = user.firstName
                lastName = user.lastName
                phone = user.phone
                passwordHash = user.passwordHash
                passwordSalt = user.passwordSalt
                createdAt = Instant.now()
            }.toAuthUser()
        } catch (cause: ExposedSQLException) {
            if (cause.cause is SQLIntegrityConstraintViolationException || cause.message?.contains("auth_users_email") == true) {
                throw DuplicateUserException("Email already exists")
            }
            throw cause
        }
    }

    override suspend fun findByEmail(email: String): AuthUser? = dbQuery {
        UserEntity.find { UserTable.email eq email }.firstOrNull()?.toAuthUser()
    }

    override suspend fun findById(id: UUID): AuthUser? = dbQuery {
        UserEntity.findById(id)?.toAuthUser()
    }

    override suspend fun updatePassword(userId: UUID, passwordHash: String, passwordSalt: String): Boolean = dbQuery {
        val user = UserEntity.findById(userId) ?: return@dbQuery false
        user.passwordHash = passwordHash
        user.passwordSalt = passwordSalt
        true
    }

    private fun UserEntity.toAuthUser(): AuthUser = AuthUser(
        id = id.value,
        email = email,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        passwordHash = passwordHash,
        passwordSalt = passwordSalt
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
