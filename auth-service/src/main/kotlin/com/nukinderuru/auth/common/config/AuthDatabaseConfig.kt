package com.nukinderuru.auth.common.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jetbrains.exposed.sql.Database

class AuthDatabaseConfig(
    private val jdbcUrl: String = env("AUTH_DATABASE_JDBC_URL", "jdbc:postgresql://localhost:5433/auth"),
    private val username: String = env("AUTH_DATABASE_USERNAME", "auth_user"),
    private val password: String = env("AUTH_DATABASE_PASSWORD", "authpass"),
    private val maximumPoolSize: Int = env("AUTH_DATABASE_MAXIMUM_POOL_SIZE", "10").toInt()
) {
    fun connect(): Database {
        val dataSource = HikariDataSource(
            HikariConfig().apply {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = this@AuthDatabaseConfig.jdbcUrl
                username = this@AuthDatabaseConfig.username
                password = this@AuthDatabaseConfig.password
                maximumPoolSize = this@AuthDatabaseConfig.maximumPoolSize
                validate()
            }
        )
        runMigrations(dataSource)
        return Database.connect(dataSource)
    }

    private fun runMigrations(dataSource: HikariDataSource) {
        dataSource.connection.use { connection ->
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            Liquibase("db/changelog/db.changelog-master.yaml", ClassLoaderResourceAccessor(), database)
                .update(Contexts(), LabelExpression())
        }
    }
}

fun env(name: String, default: String): String = System.getenv(name) ?: default

fun requiredEnv(name: String): String = System.getenv(name)
    ?: throw IllegalStateException("Environment variable $name is required")
