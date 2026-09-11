package com.nukinderuru

import com.nukinderuru.auth.AuthClient
import com.nukinderuru.auth.AuthHttpRequest
import com.nukinderuru.auth.RegisterHttpRequest
import com.nukinderuru.auth.ResetPasswordHttpRequest
import com.nukinderuru.auth.requireAuthorization
import com.nukinderuru.common.config.configureAuthorization
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthorizationTest {
    @Test
    fun `protected route rejects missing authorization header`() = testApplication {
        application { testModule(FakeAuthorizationAuthClient()) }

        val response = client.post("/protected")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Unauthorized"))
    }

    @Test
    fun `protected route rejects raw token without bearer prefix`() = testApplication {
        application { testModule(FakeAuthorizationAuthClient()) }

        val response = client.post("/protected") {
            header(HttpHeaders.Authorization, "valid-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `protected route rejects invalid bearer token`() = testApplication {
        application { testModule(FakeAuthorizationAuthClient(validTokens = emptySet())) }

        val response = client.post("/protected") {
            bearerAuth("invalid-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `protected route allows valid bearer token`() = testApplication {
        application { testModule(FakeAuthorizationAuthClient(validTokens = setOf("valid-token"))) }

        val response = client.post("/protected") {
            bearerAuth("valid-token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("protected", response.bodyAsText())
    }

    private fun io.ktor.server.application.Application.testModule(authClient: AuthClient) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(module { single<AuthClient> { authClient } })
        }
        configureAuthorization()
        routing {
            post("/protected") {
                call.respondText("protected")
            }.requireAuthorization()
        }
    }
}

private class FakeAuthorizationAuthClient(
    private val validTokens: Set<String> = setOf("valid-token"),
) : AuthClient {
    override suspend fun register(request: RegisterHttpRequest): String = "token"
    override suspend fun authenticate(request: AuthHttpRequest): String = "token"
    override suspend fun resetPassword(request: ResetPasswordHttpRequest): Boolean = true
    override suspend fun validateToken(token: String): Boolean = token in validTokens
}
