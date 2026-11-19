package com.nukinderuru.data.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jetbrains.exposed.sql.Database

class DatabaseConfig(private val config: ApplicationConfig) {
    fun createDataSource(): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                driverClassName = config.property("database.driverClassName").getString()
                jdbcUrl = config.property("database.jdbcUrl").getString()
                username = config.property("database.username").getString()
                password = config.property("database.password").getString()
                maximumPoolSize = config.property("database.maximumPoolSize").getString().toInt()
                validate()
            }
        )

    fun connect(dataSource: HikariDataSource): Database {
        return try {
            val shouldRunMigrations = config
                .property("database.runMigrations")
                .getString()
                .toBooleanStrict()

            if (shouldRunMigrations) {
                runMigrations(dataSource)
            }

            Database.connect(dataSource)
        } catch (exception: Exception) {
            dataSource.close()
            throw exception
        }
    }

    private fun runMigrations(dataSource: HikariDataSource) {
        dataSource.connection.use { connection ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase(
                "db/changelog/db.changelog-master.yaml",
                ClassLoaderResourceAccessor(),
                database,
            )
            liquibase.update(Contexts(), LabelExpression())
        }
    }
}
