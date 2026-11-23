package com.nukinderuru.domain.service

import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.repository.ProductChangeResult
import com.nukinderuru.data.repository.ProductRepository
import com.nukinderuru.domain.event.ProductUpdateBroadcaster
import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.events.ProductChangeEvent
import java.math.BigDecimal

class ProductEventService(
    private val repository: ProductRepository,
    private val broadcaster: ProductUpdateBroadcaster
) {
    suspend fun process(event: ProductChangeEvent): ProductEventProcessingResult {
        validate(event)
        return when (val result = repository.applyProductChange(event)) {
            is ProductChangeResult.Applied -> ProductEventProcessingResult.Applied(result.product)
                .also { broadcaster.publish(result.product) }
            ProductChangeResult.Duplicate -> ProductEventProcessingResult.Duplicate
            ProductChangeResult.NotFound -> ProductEventProcessingResult.NotFound
            ProductChangeResult.Stale -> ProductEventProcessingResult.Stale
        }
    }

    private fun validate(event: ProductChangeEvent) {
        if (event.oldPrice <= BigDecimal.ZERO) {
            throw ValidationException(
                ValidationConstants.fieldMustBeGreaterThanZero(ValidationConstants.FIELD_OLD_PRODUCT_PRICE)
            )
        }
        if (event.oldAvailableStock < 0) {
            throw ValidationException(
                ValidationConstants.fieldMustBeGreaterThanOrEqualToZero(ValidationConstants.FIELD_OLD_AVAILABLE_STOCK)
            )
        }
        if (event.newPrice <= BigDecimal.ZERO) {
            throw ValidationException(
                ValidationConstants.fieldMustBeGreaterThanZero(ValidationConstants.FIELD_PRODUCT_PRICE)
            )
        }
        if (event.newAvailableStock < 0) {
            throw ValidationException(
                ValidationConstants.fieldMustBeGreaterThanOrEqualToZero(ValidationConstants.FIELD_AVAILABLE_STOCK)
            )
        }
        if (event.oldPrice.compareTo(event.newPrice) == 0 &&
            event.oldAvailableStock == event.newAvailableStock
        ) {
            throw ValidationException(ValidationConstants.productChangeMustModifyPriceOrStock())
        }
    }
}