package com.nukinderuru.common.config

import com.nukinderuru.auth.AuthorizationEnabledKey
import io.ktor.server.application.Application

fun Application.configureAuthorization() {
    attributes.put(AuthorizationEnabledKey, true)
}
