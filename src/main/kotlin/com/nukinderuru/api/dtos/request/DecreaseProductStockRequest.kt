package com.nukinderuru.api.dtos.request

import kotlinx.serialization.Serializable

@Serializable
data class DecreaseProductStockRequest(
    val decreaseBy: Int
)
