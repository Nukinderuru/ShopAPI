package com.nukinderuru

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.api.routes.productEventRoutes
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import com.nukinderuru.domain.event.ProductUpdateBroadcaster
import com.nukinderuru.domain.exception.ConflictException
import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.domain.service.ProductEventService
import com.nukinderuru.events.ProductChangeEvent
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE as ServerSSE
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ProductEventRoutesTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `post product event returns no content after processing`() = testApplication {
        val service = mockk<ProductEventService>()
        val event = productChange()
        coEvery { service.process(event) } returns productResponse()
        application { testModule(service, ProductUpdateBroadcaster()) }

        val response = client.post("/api/v1/internal/product-events") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(event))
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `post stale product event returns conflict`() = testApplication {
        val service = mockk<ProductEventService>()
        val event = productChange()
        coEvery { service.process(event) } throws ConflictException("stale product event")
        application { testModule(service, ProductUpdateBroadcaster()) }

        val response = client.post("/api/v1/internal/product-events") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(event))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `post event for missing product returns not found`() = testApplication {
        val service = mockk<ProductEventService>()
        val event = productChange()
        coEvery { service.process(event) } throws NotFoundException("missing product")
        application { testModule(service, ProductUpdateBroadcaster()) }

        val response = client.post("/api/v1/internal/product-events") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(event))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `post invalid product event returns bad request`() = testApplication {
        val service = mockk<ProductEventService>()
        val event = productChange().copy(newAvailableStock = -1)
        coEvery { service.process(event) } throws ValidationException("invalid product event")
        application { testModule(service, ProductUpdateBroadcaster()) }

        val response = client.post("/api/v1/internal/product-events") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(event))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `post malformed product event returns bad request`() = testApplication {
        application { testModule(mockk(relaxed = true), ProductUpdateBroadcaster()) }

        val response = client.post("/api/v1/internal/product-events") {
            contentType(ContentType.Application.Json)
            setBody("{not-json}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `sse sends named product update with json data`() = testApplication {
        val broadcaster = ProductUpdateBroadcaster()
        application { testModule(mockk(relaxed = true), broadcaster) }
        val streamingClient = createClient { install(SSE) }

        streamingClient.sse("/api/v1/product-updates/sse") {
            withTimeout(5_000.milliseconds) { broadcaster.subscriptionCount.first { it > 0 } }
            broadcaster.publish(productResponse())
            val event = withTimeout(5_000.milliseconds) { incoming.first() }

            assertEquals("product-updated", event.event)
            assertTrue(event.data.orEmpty().contains("\"availableStock\":3"))
            assertTrue(event.data.orEmpty().contains(PRODUCT_ID.toString()))
        }
    }

    @Test
    fun `websocket sends product update without client message`() = testApplication {
        val broadcaster = ProductUpdateBroadcaster()
        application { testModule(mockk(relaxed = true), broadcaster) }
        val websocketClient = createClient { install(WebSockets) }

        websocketClient.webSocket("/api/v1/product-updates/ws") {
            withTimeout(5_000.milliseconds) { broadcaster.subscriptionCount.first { it > 0 } }
            broadcaster.publish(productResponse())
            val payload = withTimeout(5_000.milliseconds) {
                (incoming.receive() as Frame.Text).readText()
            }

            assertTrue(payload.contains("\"availableStock\":3"))
            assertTrue(payload.contains(PRODUCT_ID.toString()))
        }
    }

    private fun Application.testModule(
        service: ProductEventService,
        broadcaster: ProductUpdateBroadcaster
    ) {
        configureSerialization()
        configureStatusPages()
        install(ServerSSE)
        install(ServerWebSockets)
        install(Koin) {
            modules(
                module {
                    single { service }
                    single { broadcaster }
                }
            )
        }
        routing {
            route("/api/v1") {
                productEventRoutes()
            }
        }
    }

    private fun productChange() = ProductChangeEvent(
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

    private companion object {
        val PRODUCT_ID: UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    }
}