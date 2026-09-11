package com.nukinderuru.auth

import io.ktor.http.HttpHeaders
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import io.ktor.server.request.header
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.getKoin

class UnauthorizedException(message: String = "Unauthorized") : RuntimeException(message)

val AuthorizationEnabledKey: AttributeKey<Boolean> = AttributeKey("AuthorizationEnabled")

private val AuthorizedRoutePlugin = createRouteScopedPlugin("AuthorizedRoutePlugin") {
    onCall { call ->
        val enabled = runCatching { call.application.attributes[AuthorizationEnabledKey] }.getOrDefault(false)
        if (!enabled) {
            return@onCall
        }

        val header = call.request.header(HttpHeaders.Authorization)
            ?: throw UnauthorizedException()
        if (!header.startsWith("Bearer ")) {
            throw UnauthorizedException()
        }

        val token = header.removePrefix("Bearer ").trim()
        if (token.isEmpty()) {
            throw UnauthorizedException()
        }

        val authClient = call.application.getKoin().get<AuthClient>()
        val valid = runCatching { authClient.validateToken(token) }.getOrDefault(false)
        if (!valid) {
            throw UnauthorizedException()
        }
    }
}

fun Route.requireAuthorization(): Route = apply {
    install(AuthorizedRoutePlugin)
    markBearerAuthRequired()
}

@OptIn(ExperimentalKtorApi::class)
private fun Route.markBearerAuthRequired(): Route = describe {
    security {
        requirement("bearerAuth")
    }
}
