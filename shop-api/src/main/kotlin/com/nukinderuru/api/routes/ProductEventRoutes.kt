package com.nukinderuru.api.routes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nukinderuru.domain.event.ProductUpdateBroadcaster
import com.nukinderuru.domain.service.ProductEventService
import com.nukinderuru.events.ProductChangeEvent
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.sse
import io.ktor.server.websocket.webSocket
import io.ktor.sse.ServerSentEvent
import io.ktor.websocket.Frame
import org.koin.ktor.ext.inject

private val productUpdateObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun Route.productEventRoutes() {
    val eventService by inject<ProductEventService>()
    val broadcaster by inject<ProductUpdateBroadcaster>()

    post("/internal/product-events") {
        eventService.process(call.receive<ProductChangeEvent>())
        call.respond(HttpStatusCode.NoContent)
    }

    route("/product-updates") {
        sse("/sse") {
            broadcaster.updates.collect { product ->
                send(
                    ServerSentEvent(
                        data = productUpdateObjectMapper.writeValueAsString(product),
                        event = "product-updated"
                    )
                )
            }
        }

        webSocket("/ws") {
            broadcaster.updates.collect { product ->
                send(Frame.Text(productUpdateObjectMapper.writeValueAsString(product)))
            }
        }
    }
}
