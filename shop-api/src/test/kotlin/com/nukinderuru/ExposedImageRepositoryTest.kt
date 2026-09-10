package com.nukinderuru

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.data.db.dao.AddressTable
import com.nukinderuru.data.db.dao.ImageEntity
import com.nukinderuru.data.db.dao.ImageTable
import com.nukinderuru.data.db.dao.ProductEntity
import com.nukinderuru.data.db.dao.ProductTable
import com.nukinderuru.data.db.dao.SupplierTable
import com.nukinderuru.data.repository.ExposedImageRepository
import com.nukinderuru.data.repository.ExposedProductRepository
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExposedImageRepositoryTest {
    @Test
    fun `createForProduct stores image and assigns it to product`() = runTest {
        val database = createDatabase()
        val productRepository = ExposedProductRepository(database)
        val imageRepository = ExposedImageRepository(database)
        val productId = productRepository.create(productRequest())

        val imageId = imageRepository.createForProduct(productId, byteArrayOf(1, 2, 3))

        assertNotNull(imageId)
        val stored = imageRepository.findByProductId(productId)
        assertNotNull(stored)
        assertEquals(imageId, stored.first)
        assertContentEquals(byteArrayOf(1, 2, 3), stored.second)
    }

    @Test
    fun `createForProduct returns null for unknown product`() = runTest {
        val repository = ExposedImageRepository(createDatabase())

        assertNull(repository.createForProduct(UUID.randomUUID(), byteArrayOf(1)))
    }

    @Test
    fun `replace updates image bytes`() = runTest {
        val database = createDatabase()
        val productRepository = ExposedProductRepository(database)
        val imageRepository = ExposedImageRepository(database)
        val productId = productRepository.create(productRequest())
        val imageId = imageRepository.createForProduct(productId, byteArrayOf(1, 2, 3))!!

        val replaced = imageRepository.replace(imageId, byteArrayOf(4, 5))

        assertTrue(replaced)
        assertContentEquals(byteArrayOf(4, 5), imageRepository.findById(imageId))
    }

    @Test
    fun `deleteById removes image and clears product relation`() = runTest {
        val database = createDatabase()
        val productRepository = ExposedProductRepository(database)
        val imageRepository = ExposedImageRepository(database)
        val productId = productRepository.create(productRequest())
        val imageId = imageRepository.createForProduct(productId, byteArrayOf(1, 2, 3))!!

        val deleted = imageRepository.deleteById(imageId)

        assertTrue(deleted)
        assertNull(imageRepository.findById(imageId))
        transaction(database) {
            assertNull(ProductEntity.findById(productId)?.image)
            assertNull(ImageEntity.findById(imageId))
        }
    }

    @Test
    fun `deleteById returns false for unknown image`() = runTest {
        val repository = ExposedImageRepository(createDatabase())

        assertFalse(repository.deleteById(UUID.randomUUID()))
    }

    private fun createDatabase(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            driver = "org.h2.Driver",
        )
        transaction(database) {
            SchemaUtils.create(AddressTable, SupplierTable, ImageTable, ProductTable)
        }
        return database
    }

    private fun productRequest() = CreateProductRequest(
        name = "Toaster",
        category = "Kitchen",
        price = BigDecimal("99.99"),
        availableStock = 5,
        lastUpdateDate = LocalDate.of(2024, 1, 1),
        supplierId = null,
        imageId = null,
    )
}
