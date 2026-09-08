package com.nukinderuru.common.config

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import org.slf4j.event.Level
import java.util.UUID

fun Application.configureLogging() {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
        replyToHeader(HttpHeaders.XRequestId)
    }

    install(CallLogging) {
        level = Level.INFO
        mdc("requestId") { it.callId }
        format { call ->
            val status = call.response.status()?.value ?: "NA"
            val method = call.request.httpMethod.value
            val path = call.request.uri
            val userAgent = call.request.headers[HttpHeaders.UserAgent] ?: "unknown"
            val requestId = call.callId ?: "missing"
            "$method $path status=$status requestId=$requestId userAgent=\"$userAgent\""
        }
    }
}
