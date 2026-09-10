package com.nukinderuru.auth.domain

import com.nukinderuru.auth.data.repository.DuplicateUserException
import com.nukinderuru.auth.data.repository.NewUser
import com.nukinderuru.auth.data.repository.UserRepository
import java.security.SecureRandom

interface AuthService {
    suspend fun register(request: RegisterCommand): String
    suspend fun authenticate(email: String, password: String): String
    suspend fun validateToken(token: String): TokenPrincipal?
    suspend fun changePassword(token: String, oldPassword: String, newPassword: String): Boolean
    suspend fun resetPassword(email: String): Boolean
}

data class RegisterCommand(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val password: String
)

open class AuthException(message: String) : RuntimeException(message)
class InvalidCredentialsException : AuthException("Invalid credentials")
class InvalidAuthInputException(message: String) : AuthException(message)

class DefaultAuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: JwtTokenService,
    private val temporaryPasswordSink: (String) -> Unit = ::println,
    private val secureRandom: SecureRandom = SecureRandom()
) : AuthService {
    override suspend fun register(request: RegisterCommand): String {
        validateRegisterCommand(request)
        val password = passwordHasher.hash(request.password)
        val user = try {
            userRepository.create(
                NewUser(
                    email = request.email,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    phone = request.phone,
                    passwordHash = password.hash,
                    passwordSalt = password.salt
                )
            )
        } catch (cause: DuplicateUserException) {
            throw cause
        }
        return tokenService.issueToken(user.id, user.email)
    }

    override suspend fun authenticate(email: String, password: String): String {
        val user = userRepository.findByEmail(email) ?: throw InvalidCredentialsException()
        if (!passwordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return tokenService.issueToken(user.id, user.email)
    }

    override suspend fun validateToken(token: String): TokenPrincipal? = tokenService.validate(token)

    override suspend fun changePassword(token: String, oldPassword: String, newPassword: String): Boolean {
        if (newPassword.isBlank()) throw InvalidAuthInputException("New password must not be blank")
        val principal = validateToken(token) ?: throw InvalidCredentialsException()
        val user = userRepository.findById(principal.userId) ?: throw InvalidCredentialsException()
        if (!passwordHasher.verify(oldPassword, user.passwordSalt, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        val password = passwordHasher.hash(newPassword)
        return userRepository.updatePassword(user.id, password.hash, password.salt)
    }

    override suspend fun resetPassword(email: String): Boolean {
        if (email.isBlank()) throw InvalidAuthInputException("Email must not be blank")
        val user = userRepository.findByEmail(email) ?: return true
        val temporaryPassword = generateTemporaryPassword()
        val password = passwordHasher.hash(temporaryPassword)
        userRepository.updatePassword(user.id, password.hash, password.salt)
        temporaryPasswordSink("Temporary password for $email: $temporaryPassword")
        return true
    }

    private fun validateRegisterCommand(request: RegisterCommand) {
        if (request.email.isBlank()) throw InvalidAuthInputException("Email must not be blank")
        if (request.firstName.isBlank()) throw InvalidAuthInputException("First name must not be blank")
        if (request.lastName.isBlank()) throw InvalidAuthInputException("Last name must not be blank")
        if (request.phone.isBlank()) throw InvalidAuthInputException("Phone must not be blank")
        if (request.password.isBlank()) throw InvalidAuthInputException("Password must not be blank")
    }

    private fun generateTemporaryPassword(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return buildString {
            repeat(16) {
                append(alphabet[secureRandom.nextInt(alphabet.length)])
            }
        }
    }
}
