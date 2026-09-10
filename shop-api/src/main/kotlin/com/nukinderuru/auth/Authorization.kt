package com.nukinderuru.auth

import io.ktor.http.HttpHeaders
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import io.ktor.server.request.header
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingHandler
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
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

fun Route.authorized(build: Route.() -> Unit): Route = route("") {
    install(AuthorizedRoutePlugin)
    build()
}

fun Route.authorizedPost(path: String = "", body: RoutingHandler): Route = post(path, body).apply {
    install(AuthorizedRoutePlugin)
}

fun Route.authorizedGet(path: String = "", body: RoutingHandler): Route = get(path, body).apply {
    install(AuthorizedRoutePlugin)
}

fun Route.authorizedPatch(path: String, body: RoutingHandler): Route = patch(path, body).apply {
    install(AuthorizedRoutePlugin)
}

fun Route.authorizedPut(path: String, body: RoutingHandler): Route = put(path, body).apply {
    install(AuthorizedRoutePlugin)
}

fun Route.authorizedDelete(path: String, body: RoutingHandler): Route = delete(path, body).apply {
    install(AuthorizedRoutePlugin)
}
