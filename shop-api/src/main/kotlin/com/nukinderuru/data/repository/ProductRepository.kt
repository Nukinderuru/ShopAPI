package com.nukinderuru.data.repository

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.response.ProductResponse
import java.util.UUID

interface ProductRepository {
    suspend fun findAllAvailable(): List<ProductResponse>

    suspend fun findById(id: UUID): ProductResponse?

    suspend fun create(request: CreateProductRequest): UUID

    suspend fun decreaseStock(id: UUID, decreaseBy: Int): ProductResponse?

    suspend fun deleteById(id: UUID): Boolean
}
