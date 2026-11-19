package com.nukinderuru.generator

import com.nukinderuru.events.ProductChangeEvent
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ProductEventWorkerTest {
    @Test
    fun `generateAndPublishEvent publishes generated product change`() = runTest {
        val database = createDatabase()
        val productId = insertProduct(database)
        val publisher = RecordingProductEventPublisher()
        val worker = ProductEventWorker(
            repository = ProductEventRepository(database, Random(2)),
            publisher = publisher,
            interval = 5.seconds
        )

        worker.generateAndPublishEvent()

        assertEquals(1, publisher.events.size)
        assertEquals(productId, publisher.events.single().productId)
    }

    @Test
    fun `generateAndPublishEvent does not publish when no products exist`() = runTest {
        val publisher = RecordingProductEventPublisher()
        val worker = ProductEventWorker(
            repository = ProductEventRepository(createDatabase(), Random(2)),
            publisher = publisher,
            interval = 5.seconds
        )

        worker.generateAndPublishEvent()

        assertTrue(publisher.events.isEmpty())
    }

    private fun createDatabase(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            driver = "org.h2.Driver"
        )
        transaction(database) {
            SchemaUtils.create(ProductTable)
        }
        return database
    }

    private fun insertProduct(database: Database): UUID {
        val id = UUID.randomUUID()
        transaction(database) {
            ProductTable.insert {
                it[ProductTable.id] = id
                it[ProductTable.price] = BigDecimal("100.00")
                it[ProductTable.availableStock] = 10
                it[ProductTable.lastUpdateDate] = LocalDate.of(2024, 1, 1)
            }
        }
        return id
    }

    private class RecordingProductEventPublisher : ProductEventPublisher {
        val events = mutableListOf<ProductChangeEvent>()

        override suspend fun publish(event: ProductChangeEvent) {
            events += event
        }
    }
}