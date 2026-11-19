package com.nukinderuru.generator

import com.nukinderuru.events.ProductChangeEvent
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class HttpProductEventPublisher(
    private val client: HttpClient,
    private val endpoint: String,
    private val json: Json = Json { encodeDefaults = true }
) : ProductEventPublisher {
    override suspend fun publish(event: ProductChangeEvent) {
        val response = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(event))
        }
        check(response.status.value in 200..299) {
            "Gateway rejected product change event with status ${response.status.value}"
        }
    }
}
