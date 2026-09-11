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

    /**
     * @tag Auth
     * @description Registers a new user and returns a JWT token.
     * @body application/json RegisterHttpRequest User registration data.
     * @response 200 application/json TokenHttpResponse JWT token.
     */
    post("/register") {
        val request = call.receive<RegisterHttpRequest>()
        call.respond(TokenHttpResponse(authClient.register(request)))
    }

    /**
     * @tag Auth
     * @description Authenticates a user and returns a JWT token.
     * @body application/json AuthHttpRequest User credentials.
     * @response 200 application/json TokenHttpResponse JWT token.
     * @response 401 application/json ErrorResponse Invalid credentials.
     */
    post("/auth") {
        val request = call.receive<AuthHttpRequest>()
        call.respond(TokenHttpResponse(authClient.authenticate(request)))
    }

    /**
     * @tag Auth
     * @description Resets user password and prints a temporary password to auth-service console.
     * @body application/json ResetPasswordHttpRequest User email.
     * @response 200 application/json ResetPasswordHttpResponse Reset result.
     */
    post("/reset") {
        val request = call.receive<ResetPasswordHttpRequest>()
        call.respond(ResetPasswordHttpResponse(authClient.resetPassword(request)))
    }
}
