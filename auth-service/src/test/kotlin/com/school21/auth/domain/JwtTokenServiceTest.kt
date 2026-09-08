package com.school21.auth.domain

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtTokenServiceTest {
    @Test
    fun `issued token validates to principal`() {
        val service = Hs256JwtTokenService(
            secret = "01234567890123456789012345678901",
            ttlSeconds = 3600,
        )
        val userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

        val token = service.issueToken(userId, "user@example.com")
        val principal = service.validate(token)

        assertNotNull(principal)
        assertEquals(userId, principal.userId)
        assertEquals("user@example.com", principal.email)
    }

    @Test
    fun `malformed token is invalid`() {
        val service = Hs256JwtTokenService("01234567890123456789012345678901", 3600)

        assertNull(service.validate("not-a-jwt"))
    }
}
