package com.nukinderuru.domain.event

import com.nukinderuru.api.dtos.response.ProductResponse
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class ProductUpdateBroadcaster {
    private val mutableUpdates = MutableSharedFlow<ProductResponse>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val updates: Flow<ProductResponse> = mutableUpdates.asSharedFlow()
    val subscriptionCount: StateFlow<Int> = mutableUpdates.subscriptionCount

    suspend fun publish(product: ProductResponse) {
        mutableUpdates.emit(product)
    }
}