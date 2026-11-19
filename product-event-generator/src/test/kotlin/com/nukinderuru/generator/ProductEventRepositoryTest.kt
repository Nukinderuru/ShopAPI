package com.nukinderuru.generator

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class ProductEventRepositoryTest {
    @Test
    fun `generateRandomProductChange returns null when products table is empty`() = runTest {
        val repository = ProductEventRepository(createDatabase(), Random(1))

        val event = repository.generateRandomProductChange()

        assertNull(event)
    }

    @Test
    fun `generateRandomProductChange returns event without updating product`() = runTest {
        val database = createDatabase()
        val productId = insertProduct(database)
        val repository = ProductEventRepository(database, Random(2))

        val event = repository.generateRandomProductChange()

        val productAfterGeneration = transaction(database) {
            ProductTable.selectAll().single()
        }

        assertNotNull(event)
        assertEquals(productId, event?.productId)
        assertEquals(BigDecimal("100.00"), event?.oldPrice)
        assertEquals(10, event?.oldAvailableStock)
        assertEquals(BigDecimal("100.00"), productAfterGeneration[ProductTable.price])
        assertEquals(10, productAfterGeneration[ProductTable.availableStock])
        assertEquals(LocalDate.of(2024, 1, 1), productAfterGeneration[ProductTable.lastUpdateDate])
        event?.newPrice?.let { assertTrue(it > BigDecimal.ZERO) }
        event?.newAvailableStock?.let { assertTrue(it >= 0) }
    }

    @Test
    fun `generateRandomProductChange keeps stock non-negative`() = runTest {
        val database = createDatabase()
        insertProduct(database, availableStock = 0)
        val repository = ProductEventRepository(database, Random(3))

        val event = repository.generateRandomProductChange()

        assertNotNull(event)
        event?.newAvailableStock?.let { assertTrue(it >= 0) }
    }

    @Test
    fun `generateRandomProductChange selects one random product`() = runTest {
        val database = createDatabase()
        val firstId = insertProduct(database, price = BigDecimal("10.00"))
        val secondId = insertProduct(database, price = BigDecimal("20.00"))
        val repository = ProductEventRepository(database, Random(4))

        val event = repository.generateRandomProductChange()
        val selectedProducts = transaction(database) {
            ProductTable.selectAll()
                .filter { row -> row[ProductTable.id] == event?.productId }
        }

        assertNotNull(event)
        assertTrue(event?.productId == firstId || event?.productId == secondId)
        assertEquals(1, selectedProducts.size)
        assertNotEquals(event?.oldPrice, event?.newPrice)
    }

    @Test
    fun `generateRandomProductChange changes minimum price despite rounding`() = runTest {
        val database = createDatabase()
        insertProduct(database, price = BigDecimal("0.01"))

        repeat(20) { seed ->
            val event = ProductEventRepository(database, Random(seed)).generateRandomProductChange()

            assertNotEquals(BigDecimal("0.01"), event?.newPrice)
        }
    }

    private fun createDatabase(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            driver = "org.h2.Driver",
        )
        transaction(database) {
            SchemaUtils.create(ProductTable)
        }
        return database
    }

    private fun insertProduct(
        database: Database,
        id: UUID = UUID.randomUUID(),
        price: BigDecimal = BigDecimal("100.00"),
        availableStock: Int = 10,
    ): UUID {
        transaction(database) {
            ProductTable.insert {
                it[ProductTable.id] = id
                it[ProductTable.price] = price
                it[ProductTable.availableStock] = availableStock
                it[ProductTable.lastUpdateDate] = LocalDate.of(2024, 1, 1)
            }
        }
        return id
    }
}