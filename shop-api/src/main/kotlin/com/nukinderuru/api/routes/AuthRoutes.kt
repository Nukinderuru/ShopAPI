package com.nukinderuru.api.routes

import com.nukinderuru.auth.AuthClient
import com.nukinderuru.auth.AuthHttpRequest
import com.nukinderuru.auth.RegisterHttpRequest
import com.nukinderuru.auth.ResetPasswordHttpRequest
import com.nukinderuru.auth.ResetPasswordHttpResponse
import com.nukinderuru.auth.TokenHttpResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val authClient by inject<AuthClient>()

    post("/register") {
        val request = call.receive<RegisterHttpRequest>()
        call.respond(TokenHttpResponse(authClient.register(request)))
    }

    post("/auth") {
        val request = call.receive<AuthHttpRequest>()
        call.respond(TokenHttpResponse(authClient.authenticate(request)))
    }

    post("/reset") {
        val request = call.receive<ResetPasswordHttpRequest>()
        call.respond(ResetPasswordHttpResponse(authClient.resetPassword(request)))
    }
}
