package com.nukinderuru.domain.service

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateClientRequest
import com.nukinderuru.api.dtos.response.ClientResponse
import com.nukinderuru.api.dtos.response.CreatedClientResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.repository.ClientRepository
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.exception.ValidationException
import java.time.LocalDate
import java.util.UUID

class ClientService(private val clientRepository: ClientRepository) {
    suspend fun getAllClients(limit: Int?, offset: Long?): List<ClientResponse> {
        if (limit != null && limit <= 0) {
            throw ValidationException(ValidationConstants.queryParameterMustBeGreaterThanZero(ValidationConstants.QUERY_PARAMETER_LIMIT))
        }
        if (offset != null && offset < 0) {
            throw ValidationException(ValidationConstants.queryParameterMustBeGreaterThanOrEqualToZero(ValidationConstants.QUERY_PARAMETER_OFFSET))
        }
        return clientRepository.findAll(limit, offset)
    }

    suspend fun getClientById(id: UUID): ClientResponse {
        return clientRepository.findById(id)
            ?: throw NotFoundException(ValidationConstants.clientNotFound(id))
    }

    suspend fun getClientsByName(firstName: String, lastName: String): List<ClientResponse> {
        validateRequiredText(firstName, "firstName")
        validateRequiredText(lastName, "lastName")
        return clientRepository.findByName(firstName, lastName)
    }

    suspend fun createClient(request: CreateClientRequest): CreatedClientResponse {
        validateClientRequest(request)
        return CreatedClientResponse(clientRepository.create(request))
    }

    suspend fun changeClientAddress(id: UUID, addressRequest: AddressRequest): ClientResponse {
        validateAddress(addressRequest)
        return clientRepository.updateAddress(id, addressRequest)
            ?: throw NotFoundException(ValidationConstants.clientNotFound(id))
    }

    suspend fun deleteClient(id: UUID) {
        if (!clientRepository.deleteById(id)) {
            throw NotFoundException(ValidationConstants.clientNotFound(id))
        }
    }

    private fun validateClientRequest(request: CreateClientRequest) {
        validateRequiredText(request.clientName, ValidationConstants.FIELD_CLIENT_NAME)
        validateRequiredText(request.clientSurname, ValidationConstants.FIELD_CLIENT_SURNAME)
        validateAddress(request.address)
        validatePastOrPresent(request.birthday, ValidationConstants.FIELD_BIRTHDAY)
        validatePastOrPresent(request.registrationDate, ValidationConstants.FIELD_REGISTRATION_DATE)
        if (request.registrationDate.isBefore(request.birthday)) {
            throw ValidationException(
                ValidationConstants.fieldCannotBeEarlierThan(
                    ValidationConstants.FIELD_REGISTRATION_DATE,
                    ValidationConstants.FIELD_BIRTHDAY,
                ),
            )
        }
    }

    private fun validateAddress(addressRequest: AddressRequest) {
        validateRequiredText(addressRequest.country, ValidationConstants.FIELD_COUNTRY)
        validateRequiredText(addressRequest.city, ValidationConstants.FIELD_CITY)
        validateRequiredText(addressRequest.street, ValidationConstants.FIELD_STREET)
    }

    private fun validateRequiredText(value: String, fieldName: String) {
        if (value.isBlank()) {
            throw ValidationException(ValidationConstants.fieldMustNotBeBlank(fieldName))
        }
    }

    private fun validatePastOrPresent(value: LocalDate, fieldName: String) {
        if (value.isAfter(LocalDate.now())) {
            throw ValidationException(ValidationConstants.fieldMustNotBeInFuture(fieldName))
        }
    }
}

