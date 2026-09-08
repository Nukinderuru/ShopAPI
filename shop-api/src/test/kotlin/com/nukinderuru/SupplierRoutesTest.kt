package com.nukinderuru

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateSupplierRequest
import com.nukinderuru.api.dtos.response.CreatedSupplierResponse
import com.nukinderuru.api.dtos.response.SupplierResponse
import com.nukinderuru.api.routes.supplierRoutes
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.service.SupplierService
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupplierRoutesTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `post supplier returns 201 created id and location header`() = testApplication {
        val service = mockk<SupplierService>()
        val createdId = UUID.fromString("550e8400-e29b-41d4-a716-446655440311")
        coEvery { service.createSupplier(any()) } returns CreatedSupplierResponse(createdId)

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.post("/api/v1/suppliers") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(validRequest()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("/api/v1/suppliers/$createdId", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `get all suppliers returns 200`() = testApplication {
        val service = mockk<SupplierService>()
        coEvery { service.getAllSuppliers() } returns listOf(sampleSupplierResponse())

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/suppliers")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Acme Supplies"))
    }

    @Test
    fun `get supplier returns 404 when service throws not found`() = testApplication {
        val service = mockk<SupplierService>()
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440322")
        coEvery { service.getSupplierById(id) } throws NotFoundException(ValidationConstants.supplierNotFound(id))

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/suppliers/$id")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.supplierNotFound(id)))
    }

    @Test
    fun `patch supplier address returns updated supplier`() = testApplication {
        val service = mockk<SupplierService>()
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440333")
        val request = AddressRequest("Russia", "Saint Petersburg", "Nevsky Prospect 15")
        coEvery { service.changeSupplierAddress(id, request) } returns sampleSupplierResponse(city = "Saint Petersburg", street = "Nevsky Prospect 15")

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.patch("/api/v1/suppliers/$id/address") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Saint Petersburg"))
        coVerify(exactly = 1) { service.changeSupplierAddress(id, request) }
    }

    @Test
    fun `delete supplier returns 204`() = testApplication {
        val service = mockk<SupplierService>()
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440344")
        coEvery { service.deleteSupplier(id) } returns Unit

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.delete("/api/v1/suppliers/$id")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `get supplier returns 400 for invalid uuid`() = testApplication {
        val service = mockk<SupplierService>(relaxed = true)

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/suppliers/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.pathParameterMustBeValidUuid(ValidationConstants.PATH_PARAMETER_ID)))
    }

    private fun io.ktor.server.application.Application.testModule(service: SupplierService) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(module { single { service } })
        }
        routing {
            route("/api/v1") {
                supplierRoutes()
            }
        }
    }

    private fun validRequest() = CreateSupplierRequest(
        name = "Acme Supplies",
        address = AddressRequest("Russia", "Moscow", "Arbat 10"),
        phoneNumber = "+79990000000",
    )

    private fun sampleSupplierResponse(
        city: String = "Moscow",
        street: String = "Arbat 10",
    ) = SupplierResponse(
        id = UUID.fromString("550e8400-e29b-41d4-a716-446655440355"),
        name = "Acme Supplies",
        address = com.nukinderuru.api.dtos.response.AddressResponse(
            id = UUID.fromString("550e8400-e29b-41d4-a716-446655440356"),
            country = "Russia",
            city = city,
            street = street,
        ),
        phoneNumber = "+79990000000",
    )
}
