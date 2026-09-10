package com.nukinderuru.auth.domain

import com.nukinderuru.auth.data.repository.AuthUser
import com.nukinderuru.auth.data.repository.DuplicateUserException
import com.nukinderuru.auth.data.repository.NewUser
import com.nukinderuru.auth.data.repository.UserRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthDomainServiceTest {
    private val repository = InMemoryUserRepository()
    private val service = DefaultAuthService(
        userRepository = repository,
        passwordHasher = Pbkdf2PasswordHasher(),
        tokenService = Hs256JwtTokenService("01234567890123456789012345678901", 3600),
        temporaryPasswordSink = { repository.lastTemporaryPassword = it.substringAfterLast(' ') }
    )

    @Test
    fun `register stores hashed password and returns valid token`() = kotlinx.coroutines.test.runTest {
        val token = service.register(
            RegisterCommand(
                email = "user@example.com",
                firstName = "John",
                lastName = "Doe",
                phone = "+79991234567",
                password = "secret123"
            )
        )

        val user = repository.findByEmail("user@example.com")
        assertNotNull(user)
        assertFalse(user.passwordHash.contains("secret123"))
        assertNotNull(service.validateToken(token))
    }

    @Test
    fun `authenticate returns token for valid password`() = kotlinx.coroutines.test.runTest {
        service.register(RegisterCommand("user@example.com", "John", "Doe", "+79991234567", "secret123"))

        val token = service.authenticate("user@example.com", "secret123")

        assertNotNull(service.validateToken(token))
    }

    @Test
    fun `change password invalidates old password`() = kotlinx.coroutines.test.runTest {
        val token = service.register(RegisterCommand("user@example.com", "John", "Doe", "+79991234567", "secret123"))

        assertTrue(service.changePassword(token, "secret123", "newSecret123"))

        assertNotNull(service.authenticate("user@example.com", "newSecret123"))
        assertFailsAuthentication { service.authenticate("user@example.com", "secret123") }
    }

    @Test
    fun `reset password stores generated temporary password`() = kotlinx.coroutines.test.runTest {
        service.register(RegisterCommand("user@example.com", "John", "Doe", "+79991234567", "secret123"))

        assertTrue(service.resetPassword("user@example.com"))

        val temporaryPassword = repository.lastTemporaryPassword
        assertNotNull(temporaryPassword)
        assertNotNull(service.authenticate("user@example.com", temporaryPassword))
        assertFailsAuthentication { service.authenticate("user@example.com", "secret123") }
    }

    private suspend fun assertFailsAuthentication(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Authentication should fail")
        } catch (_: AuthException) {
        }
    }
}

private class InMemoryUserRepository : UserRepository {
    private val usersByEmail = linkedMapOf<String, AuthUser>()
    var lastTemporaryPassword: String? = null

    override suspend fun create(user: NewUser): AuthUser {
        if (usersByEmail.containsKey(user.email)) {
            throw DuplicateUserException("Email already exists")
        }
        val authUser = AuthUser(
            id = UUID.randomUUID(),
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phone = user.phone,
            passwordHash = user.passwordHash,
            passwordSalt = user.passwordSalt
        )
        usersByEmail[user.email] = authUser
        return authUser
    }

    override suspend fun findByEmail(email: String): AuthUser? = usersByEmail[email]

    override suspend fun findById(id: UUID): AuthUser? = usersByEmail.values.firstOrNull { it.id == id }

    override suspend fun updatePassword(userId: UUID, passwordHash: String, passwordSalt: String): Boolean {
        val entry = usersByEmail.entries.firstOrNull { it.value.id == userId } ?: return false
        entry.setValue(entry.value.copy(passwordHash = passwordHash, passwordSalt = passwordSalt))
        return true
    }
}
