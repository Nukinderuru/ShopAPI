package com.nukinderuru

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateClientRequest
import com.nukinderuru.api.dtos.response.AddressResponse
import com.nukinderuru.api.dtos.response.ClientResponse
import com.nukinderuru.api.dtos.response.CreatedClientResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.db.dao.Gender
import com.nukinderuru.data.repository.ClientRepository
import com.nukinderuru.domain.service.ClientService
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.exception.ValidationException
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClientServiceTest {
    private val repository = FakeClientRepository()
    private val service = ClientService(repository)

    @Test
    fun `createClient rejects blank client name`() = runTest {
        val request = validRequest().copy(clientName = "   ")

        assertFailsWith<ValidationException> {
                service.createClient(request)
        }
    }

    @Test
    fun `createClient returns only created client id`() = runTest {
        val response = service.createClient(validRequest())

        assertEquals(CreatedClientResponse(FakeClientRepository.createdClientId), response)
    }

    @Test
    fun `getAllClients rejects invalid limit`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.getAllClients(limit = 0, offset = 0)
        }

        assertEquals(
            ValidationConstants.queryParameterMustBeGreaterThanZero(ValidationConstants.QUERY_PARAMETER_LIMIT),
            exception.message,
        )
    }

    @Test
    fun `getAllClients rejects negative offset`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.getAllClients(limit = 10, offset = -1)
        }

        assertEquals(
            ValidationConstants.queryParameterMustBeGreaterThanOrEqualToZero(ValidationConstants.QUERY_PARAMETER_OFFSET),
            exception.message,
        )
    }

    @Test
    fun `getClientsByName rejects blank firstName`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.getClientsByName(" ", "Doe")
        }

        assertEquals(
            ValidationConstants.fieldMustNotBeBlank(ValidationConstants.QUERY_PARAMETER_FIRST_NAME),
            exception.message,
        )
    }

    @Test
    fun `createClient rejects registrationDate earlier than birthday`() = runTest {
        val request = validRequest().copy(
            birthday = LocalDate.of(2024, 1, 2),
            registrationDate = LocalDate.of(2024, 1, 1),
        )

        val exception = assertFailsWith<ValidationException> {
            service.createClient(request)
        }

        assertEquals(
            ValidationConstants.fieldCannotBeEarlierThan(
                ValidationConstants.FIELD_REGISTRATION_DATE,
                ValidationConstants.FIELD_BIRTHDAY,
            ),
            exception.message,
        )
    }

    @Test
    fun `getClientById returns client when found`() = runTest {
        val client = service.getClientById(FakeClientRepository.createdClientId)

        assertEquals(FakeClientRepository.existingClient.id, client.id)
        assertEquals(FakeClientRepository.existingClient.clientName, client.clientName)
    }

    @Test
    fun `getClientById throws not found when client is missing`() = runTest {
        val missingId = UUID.fromString("550e8400-e29b-41d4-a716-446655440999")

        val exception = assertFailsWith<NotFoundException> {
            service.getClientById(missingId)
        }

        assertEquals(ValidationConstants.clientNotFound(missingId), exception.message)
    }

    @Test
    fun `changeClientAddress throws not found when client is missing`() = runTest {
        val missingId = UUID.fromString("550e8400-e29b-41d4-a716-446655440998")

        val exception = assertFailsWith<NotFoundException> {
            service.changeClientAddress(
                missingId,
                AddressRequest("Russia", "Moscow", "Arbat 10"),
            )
        }

        assertEquals(ValidationConstants.clientNotFound(missingId), exception.message)
    }

    @Test
    fun `deleteClient throws not found when client is missing`() = runTest {
        val missingId = UUID.fromString("550e8400-e29b-41d4-a716-446655440997")

        val exception = assertFailsWith<NotFoundException> {
            service.deleteClient(missingId)
        }

        assertEquals(ValidationConstants.clientNotFound(missingId), exception.message)
    }

    @Test
    fun `createClient propagates unexpected repository error`() = runTest {
        repository.createException = IllegalStateException("Database unavailable")

        val exception = assertFailsWith<IllegalStateException> {
            service.createClient(validRequest())
        }

        assertEquals("Database unavailable", exception.message)
    }

    private fun validRequest() = CreateClientRequest(
        clientName = "Jane",
        clientSurname = "Doe",
        birthday = LocalDate.of(1994, 2, 10),
        gender = Gender.FEMALE,
        registrationDate = LocalDate.of(2024, 1, 1),
        address = AddressRequest(
            country = "Russia",
            city = "Moscow",
            street = "Arbat 10",
        ),
    )
}

private class FakeClientRepository : ClientRepository {
    var createException: RuntimeException? = null

    override suspend fun create(request: CreateClientRequest): UUID {
        createException?.let { throw it }
        return createdClientId
    }

    override suspend fun findById(id: UUID): ClientResponse? =
        existingClient.takeIf { it.id == id }

    override suspend fun deleteById(id: UUID): Boolean = id == createdClientId

    override suspend fun findByName(firstName: String, lastName: String): List<ClientResponse> = emptyList()

    override suspend fun findAll(limit: Int?, offset: Long?): List<ClientResponse> = emptyList()

    override suspend fun updateAddress(id: UUID, addressRequest: AddressRequest): ClientResponse? =
        existingClient.takeIf { it.id == id }?.copy(
            address = existingClient.address.copy(
                country = addressRequest.country,
                city = addressRequest.city,
                street = addressRequest.street,
            ),
        )

    companion object {
        val createdClientId: UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val existingClient = ClientResponse(
            id = createdClientId,
            clientName = "Jane",
            clientSurname = "Doe",
            birthday = LocalDate.of(1994, 2, 10),
            gender = Gender.FEMALE,
            registrationDate = LocalDate.of(2024, 1, 1),
            address = AddressResponse(
                id = UUID.fromString("550e8400-e29b-41d4-a716-446655440100"),
                country = "Russia",
                city = "Moscow",
                street = "Arbat 10",
            ),
        )
    }
}
