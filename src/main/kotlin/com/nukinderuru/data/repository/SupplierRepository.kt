package com.nukinderuru.data.repository

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateSupplierRequest
import com.nukinderuru.api.dtos.response.SupplierResponse
import java.util.UUID

interface SupplierRepository {
    suspend fun findAll(): List<SupplierResponse>

    suspend fun findById(id: UUID): SupplierResponse?

    suspend fun create(request: CreateSupplierRequest): UUID

    suspend fun updateAddress(id: UUID, addressRequest: AddressRequest): SupplierResponse?

    suspend fun deleteById(id: UUID): Boolean
}
