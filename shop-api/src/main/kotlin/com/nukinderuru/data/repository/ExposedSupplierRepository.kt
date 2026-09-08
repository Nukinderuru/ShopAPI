package com.nukinderuru.data.repository

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateSupplierRequest
import com.nukinderuru.api.dtos.response.SupplierResponse
import com.nukinderuru.common.mapper.toAddressDraft
import com.nukinderuru.common.mapper.toResponse
import com.nukinderuru.common.mapper.toSupplierDraft
import com.nukinderuru.common.mapper.updateFrom
import com.nukinderuru.data.db.dao.AddressEntity
import com.nukinderuru.data.db.dao.SupplierEntity
import com.nukinderuru.data.db.dao.SupplierTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ExposedSupplierRepository(private val database: Database) : SupplierRepository {
    override suspend fun findAll(): List<SupplierResponse> = dbQuery {
        SupplierEntity.all()
            .orderBy(SupplierTable.id to SortOrder.ASC)
            .map(SupplierEntity::toResponse)
    }

    override suspend fun findById(id: UUID): SupplierResponse? = dbQuery {
        SupplierEntity.findById(id)?.toResponse()
    }

    override suspend fun create(request: CreateSupplierRequest): UUID = dbQuery {
        val addressDraft = request.address.toAddressDraft()
        val supplierDraft = request.toSupplierDraft()

        val address = AddressEntity.new {
            updateFrom(addressDraft)
        }

        SupplierEntity.new {
            name = supplierDraft.name
            this.address = address
            phoneNumber = supplierDraft.phoneNumber
        }.id.value
    }

    override suspend fun updateAddress(id: UUID, addressRequest: AddressRequest): SupplierResponse? = dbQuery {
        val supplier = SupplierEntity.findById(id) ?: return@dbQuery null
        supplier.address.updateFrom(addressRequest.toAddressDraft())
        supplier.toResponse()
    }

    override suspend fun deleteById(id: UUID): Boolean = dbQuery {
        val supplier = SupplierEntity.findById(id) ?: return@dbQuery false
        val addressId = supplier.address.id.value
        supplier.delete()
        AddressEntity.findById(addressId)?.delete()
        true
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
