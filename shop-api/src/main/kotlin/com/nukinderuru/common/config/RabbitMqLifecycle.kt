package com.nukinderuru.common.config

import com.nukinderuru.messaging.rabbitmq.ProductEventConsumerSupervisor
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import org.koin.ktor.ext.inject

fun Application.configureRabbitMqLifecycle() {
    val supervisor by inject<ProductEventConsumerSupervisor>()
    this@configureRabbitMqLifecycle.monitor.subscribe(ApplicationStarted) { supervisor.start() }
    this@configureRabbitMqLifecycle.monitor.subscribe(ApplicationStopping) { supervisor.close() }
}