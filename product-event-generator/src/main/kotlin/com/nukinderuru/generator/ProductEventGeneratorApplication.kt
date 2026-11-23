package com.nukinderuru.generator

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

fun main() = runBlocking {
    val config = GeneratorConfig.fromEnvironment()
    config.createDataSource().use { dataSource ->
        val database = Database.connect(dataSource)

        config.createRabbitMqConnectionFactory().newConnection().use { connection ->
            connection.createChannel().use { channel ->
                channel.exchangeDeclare(config.rabbitMqExchange, "direct", true)
                ProductEventWorker(
                    repository = ProductEventRepository(database),
                    publisher = RabbitMqProductEventPublisher(
                        RabbitPublisherChannelAdapter(channel),
                        config.rabbitMqExchange,
                        config.rabbitMqRoutingKey,
                        config.rabbitMqConfirmTimeout.inWholeMilliseconds
                    ),
                    interval = config.interval
                ).run()
            }
        }
    }
}