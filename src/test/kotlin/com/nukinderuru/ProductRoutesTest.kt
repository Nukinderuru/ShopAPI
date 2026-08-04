package com.nukinderuru

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.request.DecreaseProductStockRequest
import com.nukinderuru.api.dtos.response.CreatedProductResponse
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.api.routes.productRoutes
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.service.ProductService
import com.nukinderuru.domain.exception.ValidationException
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
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

class ProductRoutesTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `post product returns 201 created id and location header`() = testApplication {
        val service = mockk<ProductService>()
        val createdId = UUID.fromString("550e8400-e29b-41d4-a716-446655440111")
        coEvery { service.createProduct(any()) } returns CreatedProductResponse(createdId)

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.post("/api/v1/products") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(validRequest()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("/api/v1/products/$createdId", response.headers[HttpHeaders.Location])
        assertTrue(response.bodyAsText().contains(createdId.toString()))
    }

    @Test
    fun `get available products returns 200`() = testApplication {
        val service = mockk<ProductService>()
        coEvery { service.getAllAvailableProducts() } returns listOf(sampleProductResponse())

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/products/available")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Toaster"))
    }

    @Test
    fun `get product returns 404 when service throws not found`() = testApplication {
        val service = mockk<ProductService>()
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440222")
        coEvery { service.getProductById(id) } throws NotFoundException(ValidationConstants.productNotFound(id))

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/products/$id")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.productNotFound(id)))
    }

    @Test
    fun `patch product stock returns updated product`() = testApplication {
        val service = mockk<ProductService>()
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440333")
        val request = DecreaseProductStockRequest(2)
        coEvery { service.decreaseProductStock(id, request) } returns sampleProductResponse().copy(availableStock = 3)

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.patch("/api/v1/products/$id/stock") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"availableStock\":3"))
        coVerify(exactly = 1) { service.decreaseProductStock(id, request) }
    }

    @Test
    fun `delete product returns 204`() = testApplication {
        val service = mockk<ProductService>()
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440444")
        coEvery { service.deleteProduct(id) } returns Unit

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.delete("/api/v1/products/$id")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `get product returns 400 for invalid uuid`() = testApplication {
        val service = mockk<ProductService>(relaxed = true)

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/products/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.pathParameterMustBeValidUuid(ValidationConstants.PATH_PARAMETER_ID)))
    }

    @Test
    fun `post product returns 400 for validation exception`() = testApplication {
        val service = mockk<ProductService>()
        coEvery { service.createProduct(any()) } throws ValidationException(
            ValidationConstants.fieldMustNotBeBlank(ValidationConstants.FIELD_PRODUCT_NAME),
        )

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.post("/api/v1/products") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(validRequest().copy(name = "")))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.fieldMustNotBeBlank(ValidationConstants.FIELD_PRODUCT_NAME)))
    }

    private fun io.ktor.server.application.Application.testModule(service: ProductService) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(module { single { service } })
        }
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
