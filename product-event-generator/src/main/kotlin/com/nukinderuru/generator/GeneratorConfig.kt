package com.nukinderuru.generator

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class GeneratorConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int,
    val interval: Duration
) {
    fun createDataSource(): HikariDataSource = HikariDataSource(
            HikariConfig().apply {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = this@GeneratorConfig.jdbcUrl
                username = this@GeneratorConfig.username
                password = this@GeneratorConfig.password
                maximumPoolSize = this@GeneratorConfig.maximumPoolSize
                validate()
            }
        )

    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): GeneratorConfig = GeneratorConfig(
            jdbcUrl = environment["DATABASE_JDBC_URL"] ?: "jdbc:postgresql://localhost:5434/shop",
            username = environment["DATABASE_USERNAME"] ?: "shop_user",
            password = environment["DATABASE_PASSWORD"] ?: "shoppass",
            maximumPoolSize = environment["DATABASE_MAXIMUM_POOL_SIZE"]?.toIntOrNull() ?: 5,
            interval = (environment["PRODUCT_EVENT_INTERVAL_SECONDS"]?.toLongOrNull() ?: 5L).seconds
        )
    }
}