package com.nukinderuru

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.request.DecreaseProductStockRequest
import com.nukinderuru.api.dtos.response.CreatedProductResponse
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.repository.ProductRepository
import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.service.ProductService
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductServiceTest {
    private val repository = FakeProductRepository()
    private val service = ProductService(repository)

    @Test
    fun `createProduct returns created product id`() = runTest {
        val response = service.createProduct(validRequest())

        assertEquals(CreatedProductResponse(FakeProductRepository.createdProductId), response)
    }

    @Test
    fun `createProduct rejects blank product name`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.createProduct(validRequest().copy(name = "   "))
        }

        assertEquals(ValidationConstants.fieldMustNotBeBlank(ValidationConstants.FIELD_PRODUCT_NAME), exception.message)
    }

    @Test
    fun `createProduct rejects non positive price`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.createProduct(validRequest().copy(price = BigDecimal.ZERO))
        }

        assertEquals(ValidationConstants.fieldMustBeGreaterThanZero(ValidationConstants.FIELD_PRODUCT_PRICE), exception.message)
    }

    @Test
    fun `createProduct rejects negative stock`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.createProduct(validRequest().copy(availableStock = -1))
        }

        assertEquals(ValidationConstants.fieldMustBeGreaterThanOrEqualToZero(ValidationConstants.FIELD_AVAILABLE_STOCK), exception.message)
    }

    @Test
    fun `getProductById returns product when found`() = runTest {
        val product = service.getProductById(FakeProductRepository.createdProductId)

        assertEquals(FakeProductRepository.existingProduct.id, product.id)
    }

    @Test
    fun `getProductById throws not found for unknown id`() = runTest {
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440999")

        val exception = assertFailsWith<NotFoundException> {
            service.getProductById(id)
        }

        assertEquals(ValidationConstants.productNotFound(id), exception.message)
    }

    @Test
    fun `decreaseProductStock rejects non positive amount`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.decreaseProductStock(FakeProductRepository.createdProductId, DecreaseProductStockRequest(0))
        }

        assertEquals(ValidationConstants.fieldMustBeGreaterThanZero(ValidationConstants.FIELD_DECREASE_BY), exception.message)
    }

    @Test
    fun `decreaseProductStock throws not found for unknown id`() = runTest {
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440998")

        val exception = assertFailsWith<NotFoundException> {
            service.decreaseProductStock(id, DecreaseProductStockRequest(1))
        }

        assertEquals(ValidationConstants.productNotFound(id), exception.message)
    }

    @Test
    fun `deleteProduct throws not found for unknown id`() = runTest {
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440997")

        val exception = assertFailsWith<NotFoundException> {
            service.deleteProduct(id)
        }

        assertEquals(ValidationConstants.productNotFound(id), exception.message)
    }

    private fun validRequest() = CreateProductRequest(
        name = "Toaster",
        category = "Kitchen",
        price = BigDecimal("99.99"),
        availableStock = 5,
        lastUpdateDate = LocalDate.of(2024, 1, 1),
        supplierId = null,
        imageId = null,
    )
}

private class FakeProductRepository : ProductRepository {
    override suspend fun create(request: CreateProductRequest): UUID = createdProductId

    override suspend fun decreaseStock(id: UUID, decreaseBy: Int): ProductResponse? =
        existingProduct.takeIf { it.id == id }?.copy(availableStock = existingProduct.availableStock - decreaseBy)

    override suspend fun findById(id: UUID): ProductResponse? = existingProduct.takeIf { it.id == id }

    override suspend fun findAllAvailable(): List<ProductResponse> = listOf(existingProduct)

    override suspend fun deleteById(id: UUID): Boolean = id == createdProductId

    companion object {
        val createdProductId: UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val existingProduct = ProductResponse(
            id = createdProductId,
            name = "Toaster",
            category = "Kitchen",
            price = BigDecimal("99.99"),
            availableStock = 5,
            lastUpdateDate = LocalDate.of(2024, 1, 1),
            supplierId = null,
            imageId = null,
        )
    }
}
