package com.nukinderuru.data.repository

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.events.ProductChangeEvent
import java.util.UUID

interface ProductRepository {
    suspend fun findAllAvailable(): List<ProductResponse>

    suspend fun findById(id: UUID): ProductResponse?

    suspend fun create(request: CreateProductRequest): UUID

    suspend fun decreaseStock(id: UUID, decreaseBy: Int): ProductResponse?

    suspend fun applyProductChange(event: ProductChangeEvent): ProductChangeResult

    suspend fun deleteById(id: UUID): Boolean
}

sealed interface ProductChangeResult {
    data class Applied(val product: ProductResponse) : ProductChangeResult

    data object Duplicate : ProductChangeResult

    data object NotFound : ProductChangeResult

    data object Stale : ProductChangeResult
}
