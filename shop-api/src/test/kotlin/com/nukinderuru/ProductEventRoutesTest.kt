package com.nukinderuru

import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.api.routes.productEventRoutes
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import com.nukinderuru.domain.event.ProductUpdateBroadcaster
import com.nukinderuru.domain.service.ProductEventService
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE as ServerSSE
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ProductEventRoutesTest {
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