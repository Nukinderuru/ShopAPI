package com.nukinderuru.generator

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

fun main() = runBlocking {
    val config = GeneratorConfig.fromEnvironment()
    config.createDataSource().use { dataSource ->
        val database = Database.connect(dataSource)
        ProductEventWorker(
            repository = ProductEventRepository(database),
            interval = config.interval
        ).run()
    }
}