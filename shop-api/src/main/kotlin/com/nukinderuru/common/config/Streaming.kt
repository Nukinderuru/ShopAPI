package com.nukinderuru.common.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sse.SSE
import io.ktor.server.websocket.WebSockets

fun Application.configureStreaming() {
    install(SSE)
    install(WebSockets) {
        pingPeriodMillis = 20_000
        timeoutMillis = 30_000
    }
}
