package com.nukinderuru.domain.service

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.request.DecreaseProductStockRequest
import com.nukinderuru.api.dtos.response.CreatedProductResponse
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.repository.ProductRepository
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.exception.ValidationException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ProductService(private val productRepository: ProductRepository) {
    suspend fun getAllAvailableProducts(): List<ProductResponse> = productRepository.findAllAvailable()

    suspend fun getProductById(id: UUID): ProductResponse =
        productRepository.findById(id) ?: throw NotFoundException(ValidationConstants.productNotFound(id))

    suspend fun createProduct(request: CreateProductRequest): CreatedProductResponse {
        validateCreateRequest(request)
        return CreatedProductResponse(productRepository.create(request))
    }

    suspend fun decreaseProductStock(id: UUID, request: DecreaseProductStockRequest): ProductResponse {
        validatePositiveIntValue(request.decreaseBy, ValidationConstants.FIELD_DECREASE_BY)
        return productRepository.decreaseStock(id, request.decreaseBy)
            ?: throw NotFoundException(ValidationConstants.productNotFound(id))
    }

    suspend fun deleteProduct(id: UUID) {
        if (!productRepository.deleteById(id)) {
            throw NotFoundException(ValidationConstants.productNotFound(id))
        }
    }

    private fun validateCreateRequest(request: CreateProductRequest) {
        validateRequiredText(request.name, ValidationConstants.FIELD_PRODUCT_NAME)
        validateRequiredText(request.category, ValidationConstants.FIELD_PRODUCT_CATEGORY)
        validatePositiveAmount(request.price, ValidationConstants.FIELD_PRODUCT_PRICE)
        if (request.availableStock < 0) {
            throw ValidationException(ValidationConstants.fieldMustBeGreaterThanOrEqualToZero(ValidationConstants.FIELD_AVAILABLE_STOCK))
        }
        validatePastOrPresent(request.lastUpdateDate, ValidationConstants.FIELD_LAST_UPDATE_DATE)
    }

    private fun validateRequiredText(value: String, fieldName: String) {
        if (value.isBlank()) {
            throw ValidationException(ValidationConstants.fieldMustNotBeBlank(fieldName))
        }
    }

    private fun validatePositiveAmount(value: BigDecimal, fieldName: String) {
        if (value <= BigDecimal.ZERO) {
            throw ValidationException(ValidationConstants.fieldMustBeGreaterThanZero(fieldName))
        }
    }

    private fun validatePositiveIntValue(value: Int, fieldName: String) {
        if (value <= 0) {
            throw ValidationException(ValidationConstants.fieldMustBeGreaterThanZero(fieldName))
        }
    }

    private fun validatePastOrPresent(value: LocalDate, fieldName: String) {
        if (value.isAfter(LocalDate.now())) {
            throw ValidationException(ValidationConstants.fieldMustNotBeInFuture(fieldName))
        }
    }
}
