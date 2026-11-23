package com.nukinderuru

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.data.repository.ProductChangeResult
import com.nukinderuru.data.repository.ProductRepository
import com.nukinderuru.domain.event.ProductUpdateBroadcaster
import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.domain.service.ProductEventProcessingResult
import com.nukinderuru.domain.service.ProductEventService
import com.nukinderuru.events.ProductChangeEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class ProductEventServiceTest {
    @Test
    fun `process returns and broadcasts applied product`() = runTest {
        val expected = productResponse()
        val broadcaster = ProductUpdateBroadcaster()
        val received = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            broadcaster.updates.first()
        }
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.Applied(expected)),
            broadcaster = broadcaster
        )

        val actual = service.process(productChange())

        assertEquals(ProductEventProcessingResult.Applied(expected), actual)
        assertEquals(expected, received.await())
    }

    @Test
    fun `process does not broadcast missing product`() = runTest {
        val broadcaster = ProductUpdateBroadcaster()
        val received = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            broadcaster.updates.first()
        }
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.NotFound),
            broadcaster = broadcaster
        )

        assertEquals(ProductEventProcessingResult.NotFound, service.process(productChange()))

        assertFalse(received.isCompleted)
        received.cancel()
    }

    @Test
    fun `process returns duplicate without looking up or broadcasting product`() = runTest {
        val broadcaster = ProductUpdateBroadcaster()
        val received = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            broadcaster.updates.first()
        }
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.Duplicate),
            broadcaster = broadcaster
        )

        assertEquals(ProductEventProcessingResult.Duplicate, service.process(productChange()))

        assertFalse(received.isCompleted)
        received.cancel()
    }

    @Test
    fun `process does not broadcast stale product change`() = runTest {
        val broadcaster = ProductUpdateBroadcaster()
        val received = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            broadcaster.updates.first()
        }
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.Stale),
            broadcaster = broadcaster
        )

        assertEquals(ProductEventProcessingResult.Stale, service.process(productChange()))

        assertFalse(received.isCompleted)
        received.cancel()
    }

    @Test
    fun `process rejects non-positive new price`() = runTest {
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.Applied(productResponse())),
            broadcaster = ProductUpdateBroadcaster()
        )

        assertFailsWith<ValidationException> {
            service.process(productChange().copy(newPrice = BigDecimal.ZERO))
        }
    }

    @Test
    fun `process rejects negative new stock`() = runTest {
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.Applied(productResponse())),
            broadcaster = ProductUpdateBroadcaster()
        )

        assertFailsWith<ValidationException> {
            service.process(productChange().copy(newAvailableStock = -1))
        }
    }

    @Test
    fun `process rejects non-positive old price`() = runTest {
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.Applied(productResponse())),
            broadcaster = ProductUpdateBroadcaster()
        )

        assertFailsWith<ValidationException> {
            service.process(productChange().copy(oldPrice = BigDecimal.ZERO))
        }
    }

    @Test
    fun `process rejects negative old stock`() = runTest {
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.Applied(productResponse())),
            broadcaster = ProductUpdateBroadcaster()
        )

        assertFailsWith<ValidationException> {
            service.process(productChange().copy(oldAvailableStock = -1))
        }
    }

    @Test
    fun `process rejects event that changes neither price nor stock`() = runTest {
        val service = ProductEventService(
            repository = StubProductRepository(ProductChangeResult.Applied(productResponse())),
            broadcaster = ProductUpdateBroadcaster()
        )
        val event = productChange().copy(
            newPrice = BigDecimal("99.99"),
            newAvailableStock = 5
        )

        assertFailsWith<ValidationException> { service.process(event) }
    }

    private fun productChange() = ProductChangeEvent(
        eventId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        productId = PRODUCT_ID,
        oldPrice = BigDecimal("99.99"),
        newPrice = BigDecimal("109.99"),
        oldAvailableStock = 5,
        newAvailableStock = 3,
        occurredAt = Instant.parse("2026-10-19T12:00:00Z")
    )

    private fun productResponse() = ProductResponse(
        id = PRODUCT_ID,
        name = "Toaster",
        category = "Kitchen",
        price = BigDecimal("109.99"),
        availableStock = 3,
        lastUpdateDate = LocalDate.of(2026, 10, 19),
        supplierId = null,
        imageId = null
    )

    private class StubProductRepository(
        private val changeResult: ProductChangeResult
    ) : ProductRepository {
        override suspend fun applyProductChange(event: ProductChangeEvent): ProductChangeResult = changeResult
        override suspend fun findAllAvailable(): List<ProductResponse> = emptyList()
        override suspend fun findById(id: UUID): ProductResponse? = null
        override suspend fun create(request: CreateProductRequest): UUID = error("Not used")
        override suspend fun decreaseStock(id: UUID, decreaseBy: Int): ProductResponse? = null
        override suspend fun deleteById(id: UUID): Boolean = false
    }

    private companion object {
        val PRODUCT_ID: UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    }
}