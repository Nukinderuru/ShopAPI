package com.nukinderuru.auth.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    private val hasher = Pbkdf2PasswordHasher()

    @Test
    fun `hash uses unique salts and verifies original password`() {
        val first = hasher.hash("secret123")
        val second = hasher.hash("secret123")

        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
        assertTrue(hasher.verify("secret123", first.salt, first.hash))
        assertFalse(hasher.verify("wrong", first.salt, first.hash))
    }
}
