package com.nukinderuru

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.response.CreatedProductResponse
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.api.routes.productRoutes
import com.nukinderuru.auth.AuthClient
import com.nukinderuru.auth.AuthHttpRequest
import com.nukinderuru.auth.RegisterHttpRequest
import com.nukinderuru.auth.ResetPasswordHttpRequest
import com.nukinderuru.common.config.configureAuthorization
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import com.nukinderuru.domain.service.ProductService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductAuthorizationTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `get available products remains public`() = testApplication {
        val service = mockk<ProductService>()
        coEvery { service.getAllAvailableProducts() } returns listOf(sampleProductResponse())
        application { testModule(service, FakeProductAuthClient()) }

        val response = client.get("/api/v1/products/available")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Toaster"))
    }

    @Test
    fun `post product rejects missing bearer token`() = testApplication {
        val service = mockk<ProductService>(relaxed = true)
        application { testModule(service, FakeProductAuthClient()) }

        val response = client.post("/api/v1/products") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(validRequest()))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        coVerify(exactly = 0) { service.createProduct(any()) }
    }

    @Test
    fun `post product allows valid bearer token`() = testApplication {
        val service = mockk<ProductService>()
        val createdId = UUID.fromString("550e8400-e29b-41d4-a716-446655440111")
        coEvery { service.createProduct(any()) } returns CreatedProductResponse(createdId)
        application { testModule(service, FakeProductAuthClient(validTokens = setOf("valid-token"))) }

        val response = client.post("/api/v1/products") {
            bearerAuth("valid-token")
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(validRequest()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        coVerify(exactly = 1) { service.createProduct(any()) }
    }

    private fun io.ktor.server.application.Application.testModule(
        service: ProductService,
        authClient: AuthClient,
    ) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(
                module {
                    single { service }
                    single<AuthClient> { authClient }
                },
            )
        }
        configureAuthorization()
        routing {
            route("/api/v1") {
                productRoutes()
            }
        }
    }

    private fun validRequest() = CreateProductRequest(
        name = "Toaster",
        category = "Kitchen",
        price = BigDecimal("99.99"),
        availableStock = 5,
        lastUpdateDate = LocalDate.of(2024, 1, 1),
        supplierId = null,
        imageId = null,
    )

    private fun sampleProductResponse() = ProductResponse(
        id = UUID.fromString("550e8400-e29b-41d4-a716-446655440555"),
        name = "Toaster",
        category = "Kitchen",
        price = BigDecimal("99.99"),
        availableStock = 5,
        lastUpdateDate = LocalDate.of(2024, 1, 1),
        supplierId = null,
        imageId = null,
    )
}

private class FakeProductAuthClient(
    private val validTokens: Set<String> = setOf("valid-token"),
) : AuthClient {
    override suspend fun register(request: RegisterHttpRequest): String = "token"
    override suspend fun authenticate(request: AuthHttpRequest): String = "token"
    override suspend fun resetPassword(request: ResetPasswordHttpRequest): Boolean = true
    override suspend fun validateToken(token: String): Boolean = token in validTokens
}
