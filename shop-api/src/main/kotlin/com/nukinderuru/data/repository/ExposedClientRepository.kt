package com.nukinderuru.data.repository

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateClientRequest
import com.nukinderuru.api.dtos.response.ClientResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.common.mapper.toAddressDraft
import com.nukinderuru.common.mapper.toClientDraft
import com.nukinderuru.common.mapper.toResponse
import com.nukinderuru.common.mapper.updateFrom
import com.nukinderuru.data.db.dao.AddressEntity
import com.nukinderuru.data.db.dao.ClientEntity
import com.nukinderuru.data.db.dao.ClientTable
import com.nukinderuru.domain.exception.ValidationException
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ExposedClientRepository(private val database: Database) : ClientRepository {
    override suspend fun findAll(limit: Int?, offset: Long?): List<ClientResponse> = dbQuery {
        val baseQuery = ClientEntity.all().orderBy(ClientTable.id to SortOrder.ASC)
        when {
            limit != null && offset != null -> baseQuery.limit(limit, offset).map(ClientEntity::toResponse)
            limit != null -> baseQuery.limit(limit).map(ClientEntity::toResponse)
            offset != null -> {
                if (offset > Int.MAX_VALUE) {
                    throw ValidationException(ValidationConstants.queryParameterTooLarge(ValidationConstants.QUERY_PARAMETER_OFFSET))
                }
                baseQuery.drop(offset.toInt()).map(ClientEntity::toResponse)
            }

            else -> baseQuery.map(ClientEntity::toResponse)
        }
    }

    override suspend fun findById(id: UUID): ClientResponse? = dbQuery {
        ClientEntity.findById(id)?.toResponse()
    }

    override suspend fun findByName(firstName: String, lastName: String): List<ClientResponse> = dbQuery {
        ClientEntity.find {
            (ClientTable.clientName eq firstName.trim()) and (ClientTable.clientSurname eq lastName.trim())
        }
            .orderBy(ClientTable.id to SortOrder.ASC)
            .map(ClientEntity::toResponse)
    }

    override suspend fun create(request: CreateClientRequest): UUID = dbQuery {
        val addressDraft = request.address.toAddressDraft()
        val clientDraft = request.toClientDraft()
        val address = AddressEntity.new {
            updateFrom(addressDraft)
        }

        ClientEntity.new {
            clientName = clientDraft.clientName
            clientSurname = clientDraft.clientSurname
            birthday = clientDraft.birthday
            gender = clientDraft.gender
            registrationDate = clientDraft.registrationDate
            this.address = address
        }.id.value
    }

    override suspend fun updateAddress(id: UUID, addressRequest: AddressRequest): ClientResponse? = dbQuery {
        val client = ClientEntity.findById(id) ?: return@dbQuery null
        client.address.updateFrom(addressRequest.toAddressDraft())
        client.toResponse()
    }

    override suspend fun deleteById(id: UUID): Boolean = dbQuery {
        val entity = ClientEntity.findById(id) ?: return@dbQuery false
        val addressId = entity.address.id.value
        entity.delete()
        AddressEntity.findById(addressId)?.delete()
        true
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
