package com.nukinderuru.generator

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

fun main() = runBlocking {
    val config = GeneratorConfig.fromEnvironment()
    config.createDataSource().use { dataSource ->
        val database = Database.connect(dataSource)

        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = config.httpTimeout.inWholeMilliseconds
                connectTimeoutMillis = config.httpTimeout.inWholeMilliseconds
            }
        }.use { httpClient ->
            ProductEventWorker(
                repository = ProductEventRepository(database),
                publisher = HttpProductEventPublisher(
                    httpClient,
                    config.gatewayUrl
                ),
                interval = config.interval
            ).run()
        }
    }
}