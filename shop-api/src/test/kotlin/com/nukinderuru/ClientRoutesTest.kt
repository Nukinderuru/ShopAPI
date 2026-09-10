package com.nukinderuru

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateClientRequest
import com.nukinderuru.api.dtos.response.AddressResponse
import com.nukinderuru.api.dtos.response.ClientResponse
import com.nukinderuru.api.dtos.response.CreatedClientResponse
import com.nukinderuru.api.routes.clientRoutes
import com.nukinderuru.common.config.configureRouting
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.db.dao.Gender
import com.nukinderuru.domain.service.ClientService
import com.nukinderuru.domain.exception.NotFoundException
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
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientRoutesTest {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `get clients returns 200 and response body`() = testApplication {
        val service = mockk<ClientService>()
        coEvery { service.getAllClients(2, 1) } returns listOf(sampleClientResponse())

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/clients?limit=2&offset=1")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(sampleClientResponse().id.toString()))
        coVerify(exactly = 1) { service.getAllClients(2, 1) }
    }

    @Test
    fun `get clients returns 400 for invalid limit`() = testApplication {
        val service = mockk<ClientService>(relaxed = true)
        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/clients?limit=abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.queryParameterMustBeInteger(ValidationConstants.QUERY_PARAMETER_LIMIT)))
    }

    @Test
    fun `post client returns 201 created id and location header`() = testApplication {
        val service = mockk<ClientService>()
        val createdId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        coEvery { service.createClient(any()) } returns CreatedClientResponse(createdId)

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.post("/api/v1/clients") {
            contentType(ContentType.Application.Json)
            setBody(
                objectMapper.writeValueAsString(
                    CreateClientRequest(
                        clientName = "John",
                        clientSurname = "Doe",
                        birthday = LocalDate.of(1995, 5, 10),
                        gender = Gender.MALE,
                        registrationDate = LocalDate.of(2024, 1, 15),
                        address = AddressRequest("Russia", "Moscow", "Tverskaya 1"),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("/api/v1/clients/$createdId", response.headers[HttpHeaders.Location])
        assertTrue(response.bodyAsText().contains(createdId.toString()))
        coVerify(exactly = 1) { service.createClient(any()) }
    }

    @Test
    fun `post client returns 400 for validation exception`() = testApplication {
        val service = mockk<ClientService>()
        coEvery { service.createClient(any()) } throws ValidationException(
            ValidationConstants.fieldMustNotBeBlank(ValidationConstants.FIELD_CLIENT_NAME),
        )

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.post("/api/v1/clients") {
            contentType(ContentType.Application.Json)
            setBody(
                objectMapper.writeValueAsString(
                    CreateClientRequest(
                        clientName = "",
                        clientSurname = "Doe",
                        birthday = LocalDate.of(1995, 5, 10),
                        gender = Gender.MALE,
                        registrationDate = LocalDate.of(2024, 1, 15),
                        address = AddressRequest("Russia", "Moscow", "Tverskaya 1"),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.fieldMustNotBeBlank(ValidationConstants.FIELD_CLIENT_NAME)))
    }

    @Test
    fun `get clients search returns 400 when firstName is missing`() = testApplication {
        val service = mockk<ClientService>(relaxed = true)
        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/clients/search?lastName=Doe")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.queryParameterRequired(ValidationConstants.QUERY_PARAMETER_FIRST_NAME)))
    }

    @Test
    fun `get client by id returns 404 when service throws not found`() = testApplication {
        val service = mockk<ClientService>()
        val clientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440999")
        coEvery { service.getClientById(clientId) } throws NotFoundException(ValidationConstants.clientNotFound(clientId))

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/clients/$clientId")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.clientNotFound(clientId)))
    }

    @Test
    fun `delete client returns 204`() = testApplication {
        val service = mockk<ClientService>()
        val clientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        coEvery { service.deleteClient(clientId) } returns Unit

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.delete("/api/v1/clients/$clientId")

        assertEquals(HttpStatusCode.NoContent, response.status)
        coVerify(exactly = 1) { service.deleteClient(clientId) }
    }

    @Test
    fun `patch client address returns updated client`() = testApplication {
        val service = mockk<ClientService>()
        val clientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val request = AddressRequest("Russia", "Saint Petersburg", "Nevsky Prospect 15")
        val updated = sampleClientResponse().copy(
            address = sampleClientResponse().address.copy(city = "Saint Petersburg", street = "Nevsky Prospect 15"),
        )
        coEvery { service.changeClientAddress(clientId, request) } returns updated

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.patch("/api/v1/clients/$clientId/address") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Saint Petersburg"))
        coVerify(exactly = 1) { service.changeClientAddress(clientId, request) }
    }

    @Test
    fun `get client by id returns 400 for invalid uuid`() = testApplication {
        val service = mockk<ClientService>(relaxed = true)
        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/clients/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.pathParameterMustBeValidUuid(ValidationConstants.PATH_PARAMETER_ID)))
    }

    @Test
    fun `get clients returns 500 for unexpected exception`() = testApplication {
        val service = mockk<ClientService>()
        coEvery { service.getAllClients(null, null) } throws IllegalStateException("boom")

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/clients")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.UNEXPECTED_SERVER_ERROR))
    }

    @Test
    fun `native openapi and swagger endpoints are reachable`() = testApplication {
        val service = mockk<ClientService>(relaxed = true)

        environment { config = MapApplicationConfig() }
        application {
            configureSerialization()
            configureStatusPages()
            install(Koin) {
                modules(module { single { service } })
            }
            configureRouting()
        }

        val swaggerResponse = client.get("/swagger")
        val openApiResponse = client.get("/openapi")

        assertEquals(HttpStatusCode.OK, swaggerResponse.status)
        assertEquals(HttpStatusCode.OK, openApiResponse.status)
    }

    private fun io.ktor.server.application.Application.testModule(service: ClientService) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(module { single { service } })
        }
        routing {
            route("/api/v1") {
                clientRoutes()
            }
        }
    }

    private fun sampleClientResponse(): ClientResponse = ClientResponse(
        id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
        clientName = "John",
        clientSurname = "Doe",
        birthday = LocalDate.of(1995, 5, 10),
        gender = Gender.MALE,
        registrationDate = LocalDate.of(2024, 1, 15),
        address = AddressResponse(
            id = UUID.fromString("550e8400-e29b-41d4-a716-446655440100"),
            country = "Russia",
            city = "Moscow",
            street = "Tverskaya 1",
        ),
    )
}
