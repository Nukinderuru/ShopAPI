package com.school21.auth.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID

interface JwtTokenService {
    fun issueToken(userId: UUID, email: String): String
    fun validate(token: String): TokenPrincipal?
}

data class TokenPrincipal(
    val userId: UUID,
    val email: String,
)

class Hs256JwtTokenService(
    secret: String,
    private val ttlSeconds: Long,
    private val clock: Clock = Clock.systemUTC(),
) : JwtTokenService {
    private val algorithm = Algorithm.HMAC256(secret)

    override fun issueToken(userId: UUID, email: String): String {
        val issuedAt = Instant.now(clock)
        return JWT.create()
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withIssuedAt(Date.from(issuedAt))
            .withExpiresAt(Date.from(issuedAt.plusSeconds(ttlSeconds)))
            .sign(algorithm)
    }

    override fun validate(token: String): TokenPrincipal? = runCatching {
        val verifier = JWT.require(algorithm).build()
        val decoded = verifier.verify(token)
        TokenPrincipal(
            userId = UUID.fromString(decoded.subject),
            email = decoded.getClaim("email").asString(),
        )
    }.getOrNull()
}
