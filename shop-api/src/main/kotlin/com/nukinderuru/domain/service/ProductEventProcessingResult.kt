package com.nukinderuru.domain.service

import com.nukinderuru.api.dtos.response.ProductResponse

sealed interface ProductEventProcessingResult {
    data class Applied(val product: ProductResponse) : ProductEventProcessingResult

    data object Duplicate : ProductEventProcessingResult

    data object NotFound : ProductEventProcessingResult

    data object Stale : ProductEventProcessingResult
}