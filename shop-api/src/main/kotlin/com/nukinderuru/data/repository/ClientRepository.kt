package com.nukinderuru.data.repository

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateClientRequest
import com.nukinderuru.api.dtos.response.ClientResponse
import java.util.UUID

interface ClientRepository {
    suspend fun findAll(limit: Int?, offset: Long?): List<ClientResponse>

    suspend fun findById(id: UUID): ClientResponse?

    suspend fun findByName(firstName: String, lastName: String): List<ClientResponse>

    suspend fun create(request: CreateClientRequest): UUID

    suspend fun updateAddress(id: UUID, addressRequest: AddressRequest): ClientResponse?

    suspend fun deleteById(id: UUID): Boolean
}
