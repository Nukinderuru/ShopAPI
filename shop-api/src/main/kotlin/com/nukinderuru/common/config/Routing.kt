package com.nukinderuru.common.config

import com.nukinderuru.api.routes.clientRoutes
import com.nukinderuru.api.routes.authRoutes
import com.nukinderuru.api.routes.imageRoutes
import com.nukinderuru.api.routes.productRoutes
import com.nukinderuru.api.routes.supplierRoutes
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.plugin
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.response.respondRedirect
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        swaggerUI(path = "swagger") {
            info = OpenApiInfo("Shop API", "1.0.0")
            source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json) {
                plugin(RoutingRoot.Plugin).descendants()
            }
        }

        get("/swagger/index.html") {
            call.respondRedirect("/swagger#/", permanent = false)
        }

        openAPI(path = "openapi") {
            info = OpenApiInfo("Shop API", "1.0.0")
            source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json) {
                plugin(RoutingRoot.Plugin).descendants()
            }
        }

        route("/api/v1") {
            authRoutes()
            clientRoutes()
            imageRoutes()
            productRoutes()
            supplierRoutes()
        }
    }
}
