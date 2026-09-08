package com.school21.auth.domain

import java.security.SecureRandom
import java.security.MessageDigest
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

interface PasswordHasher {
    fun hash(password: String): HashedPassword
    fun verify(password: String, salt: String, expectedHash: String): Boolean
}

data class HashedPassword(
    val hash: String,
    val salt: String,
)

class Pbkdf2PasswordHasher(
    private val iterations: Int = 120_000,
    private val keyLength: Int = 256,
    private val secureRandom: SecureRandom = SecureRandom(),
) : PasswordHasher {
    override fun hash(password: String): HashedPassword {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return HashedPassword(
            hash = hashWithSalt(password, salt),
            salt = Base64.getEncoder().encodeToString(salt),
        )
    }

    override fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val saltBytes = Base64.getDecoder().decode(salt)
        val actualHash = hashWithSalt(password, saltBytes)
        return MessageDigest.isEqual(actualHash.toByteArray(), expectedHash.toByteArray())
    }

    private fun hashWithSalt(password: String, salt: ByteArray): String {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        return Base64.getEncoder().encodeToString(factory.generateSecret(spec).encoded)
    }
}
