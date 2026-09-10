package com.nukinderuru

import com.nukinderuru.api.routes.authRoutes
import com.nukinderuru.auth.AuthClient
import com.nukinderuru.auth.AuthHttpRequest
import com.nukinderuru.auth.RegisterHttpRequest
import com.nukinderuru.auth.ResetPasswordHttpRequest
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureRouting
import com.nukinderuru.common.config.configureStatusPages
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.grpc.Status
import io.ktor.server.application.install
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest {
    @Test
    fun `root register is not registered`() = testApplication {
        val authClient = FakeAuthClient(registerToken = "register-token")
        application { productionRoutingModule(authClient) }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email":"user@example.com",
                  "firstName":"John",
                  "lastName":"Doe",
                  "phone":"+79991234567",
                  "password":"secret123"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(null, authClient.lastRegisterRequest)
    }

    @Test
    fun `api v1 register proxies to auth client and returns token`() = testApplication {
        val authClient = FakeAuthClient(registerToken = "register-token")
        application { testModule(authClient) }

        val response = client.post("/api/v1/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email":"user@example.com",
                  "firstName":"John",
                  "lastName":"Doe",
                  "phone":"+79991234567",
                  "password":"secret123"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("register-token"))
        assertEquals("user@example.com", authClient.lastRegisterRequest?.email)
    }

    @Test
    fun `api v1 auth proxies to auth client and returns token`() = testApplication {
        val authClient = FakeAuthClient(authToken = "auth-token")
        application { testModule(authClient) }

        val response = client.post("/api/v1/auth") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com","password":"secret123"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("auth-token"))
        assertEquals("user@example.com", authClient.lastAuthRequest?.email)
    }

    @Test
    fun `api v1 reset proxies to auth client and returns success`() = testApplication {
        val authClient = FakeAuthClient(resetSuccess = true)
        application { testModule(authClient) }

        val response = client.post("/api/v1/reset") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("true"))
        assertEquals("user@example.com", authClient.lastResetRequest?.email)
    }

    @Test
    fun `auth returns 401 when auth service rejects credentials`() = testApplication {
        val authClient = FakeAuthClient(authFailure = Status.UNAUTHENTICATED.asRuntimeException())
        application { testModule(authClient) }

        val response = client.post("/api/v1/auth") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `auth returns 401 when grpc kotlin client throws status exception`() = testApplication {
        val authClient = FakeAuthClient(authFailure = Status.UNAUTHENTICATED.withDescription("Invalid credentials").asException())
        application { testModule(authClient) }

        val response = client.post("/api/v1/auth") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Invalid credentials"))
    }

    private fun io.ktor.server.application.Application.testModule(authClient: AuthClient) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(module { single<AuthClient> { authClient } })
        }
        routing {
            route("/api/v1") {
                authRoutes()
            }
        }
    }

    private fun io.ktor.server.application.Application.productionRoutingModule(authClient: AuthClient) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(module { single<AuthClient> { authClient } })
        }
        configureRouting()
    }
}

private class FakeAuthClient(
    private val registerToken: String = "token",
    private val authToken: String = "token",
    private val resetSuccess: Boolean = true,
    private val authFailure: Exception? = null,
) : AuthClient {
    var lastRegisterRequest: RegisterHttpRequest? = null
    var lastAuthRequest: AuthHttpRequest? = null
    var lastResetRequest: ResetPasswordHttpRequest? = null

    override suspend fun register(request: RegisterHttpRequest): String {
        lastRegisterRequest = request
        return registerToken
    }

    override suspend fun authenticate(request: AuthHttpRequest): String {
        authFailure?.let { throw it }
        lastAuthRequest = request
        return authToken
    }

    override suspend fun resetPassword(request: ResetPasswordHttpRequest): Boolean {
        lastResetRequest = request
        return resetSuccess
    }

    override suspend fun validateToken(token: String): Boolean = token == "valid-token"
}
