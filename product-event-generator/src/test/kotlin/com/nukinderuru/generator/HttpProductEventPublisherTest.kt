package com.nukinderuru.generator

import com.nukinderuru.events.ProductChangeEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteReadPacket
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readText
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HttpProductEventPublisherTest {
    @Test
    fun `publish posts product change as json to gateway`() = runTest {
        var capturedRequest: HttpRequestData? = null
        var capturedBody = ""
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    capturedRequest = request
                    capturedBody = request.body.toByteReadPacket().readText()
                    respond(
                        content = ByteReadChannel.Empty,
                        status = HttpStatusCode.NoContent,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }
        val event = ProductChangeEvent(
            productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            oldPrice = BigDecimal("100.00"),
            newPrice = BigDecimal("115.00"),
            oldAvailableStock = 10,
            newAvailableStock = 7,
            occurredAt = Instant.parse("2026-10-19T12:00:00Z")
        )

        HttpProductEventPublisher(client, "http://gateway:8080/api/v1/internal/product-events").publish(event)

        val request = requireNotNull(capturedRequest)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("http://gateway:8080/api/v1/internal/product-events", request.url.toString())
        assertEquals("application/json", request.body.contentType?.withoutParameters().toString())
        assertTrue(capturedBody.contains("\"productId\":\"550e8400-e29b-41d4-a716-446655440000\""), capturedBody)
        assertTrue(capturedBody.contains("\"oldPrice\":\"100.00\""), capturedBody)
        assertTrue(capturedBody.contains("\"newPrice\":\"115.00\""), capturedBody)
        assertTrue(capturedBody.contains("\"newAvailableStock\":7"), capturedBody)
        assertTrue(capturedBody.contains("\"occurredAt\":\"2026-10-19T12:00:00Z\""), capturedBody)
    }

    @Test
    fun `publish fails when gateway rejects event`() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = ByteReadChannel.Empty,
                        status = HttpStatusCode.Conflict
                    )
                }
            }
        }
        val event = ProductChangeEvent(
            productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            oldPrice = BigDecimal("100.00"),
            newPrice = BigDecimal("115.00"),
            oldAvailableStock = 10,
            newAvailableStock = 7,
            occurredAt = Instant.parse("2026-10-19T12:00:00Z")
        )

        assertFailsWith<IllegalStateException> {
            HttpProductEventPublisher(client, "http://gateway/events").publish(event)
        }
    }
}