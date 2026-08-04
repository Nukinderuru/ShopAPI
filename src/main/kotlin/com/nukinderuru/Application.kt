package com.nukinderuru

import com.nukinderuru.common.config.configureDependencyInjection
import com.nukinderuru.common.config.configureLogging
import com.nukinderuru.common.config.configureRouting
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import io.ktor.server.application.Application

fun Application.module() {
    configureLogging()
    configureSerialization()
    configureStatusPages()
    configureDependencyInjection()
    configureRouting()
}
