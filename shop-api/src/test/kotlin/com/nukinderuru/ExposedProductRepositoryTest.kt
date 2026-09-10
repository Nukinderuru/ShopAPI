package com.nukinderuru

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.db.dao.AddressEntity
import com.nukinderuru.data.db.dao.AddressTable
import com.nukinderuru.data.db.dao.ImageEntity
import com.nukinderuru.data.db.dao.ImageTable
import com.nukinderuru.data.db.dao.ProductEntity
import com.nukinderuru.data.db.dao.ProductTable
import com.nukinderuru.data.db.dao.SupplierEntity
import com.nukinderuru.data.db.dao.SupplierTable
import com.nukinderuru.data.repository.ExposedProductRepository
import com.nukinderuru.domain.exception.ValidationException
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ExposedProductRepositoryTest {

    @Test
    fun `create stores trimmed values and returns product`() = runTest {
        val repository = createRepository()

        val id = repository.create(productRequest(name = "  Toaster  ", category = "  Kitchen  "))

        val product = repository.findById(id)

        assertNotNull(product)
        assertEquals("Toaster", product.name)
        assertEquals("Kitchen", product.category)
    }

    @Test
    fun `create stores optional supplier and image ids when present`() = runTest {
        val database = createDatabase()
        val repository = ExposedProductRepository(database)
        val supplierId = transaction(database) {
            val address = AddressEntity.new {
                country = "Russia"
                city = "Moscow"
                street = "Arbat 10"
            }
            SupplierEntity.new {
                name = "Acme"
                this.address = address
                phoneNumber = "+79990000000"
            }.id.value
        }
        val imageId = transaction(database) {
            ImageEntity.new {
                image = byteArrayOf(1, 2, 3)
            }.id.value
        }

        val id = repository.create(productRequest(supplierId = supplierId, imageId = imageId))
        val product = repository.findById(id)

        assertNotNull(product)
        assertEquals(supplierId, product.supplierId)
        assertEquals(imageId, product.imageId)
    }

    @Test
    fun `findById returns null for unknown id`() = runTest {
        val repository = createRepository()

        assertNull(repository.findById(UUID.randomUUID()))
    }

    @Test
    fun `findAllAvailable returns only products with stock greater than zero`() = runTest {
        val repository = createRepository()
        repository.create(productRequest(name = "Available", availableStock = 5))
        repository.create(productRequest(name = "SoldOut", availableStock = 0))

        val products = repository.findAllAvailable()

        assertEquals(1, products.size)
        assertEquals("Available", products.single().name)
    }

    @Test
    fun `decreaseStock updates stock and update date`() = runTest {
        val repository = createRepository()
        val id = repository.create(productRequest(availableStock = 5, lastUpdateDate = LocalDate.of(2024, 1, 1)))

        val updated = repository.decreaseStock(id, 2)

        assertNotNull(updated)
        assertEquals(3, updated.availableStock)
        assertEquals(LocalDate.now(), updated.lastUpdateDate)
    }

    @Test
    fun `decreaseStock throws when amount exceeds stock`() = runTest {
        val repository = createRepository()
        val id = repository.create(productRequest(availableStock = 1))

        val exception = assertFailsWith<ValidationException> {
            repository.decreaseStock(id, 2)
        }

        assertEquals(ValidationConstants.productDecreaseExceedsStock(1, 2), exception.message)
    }

    @Test
    fun `decreaseStock returns null for unknown product`() = runTest {
        val repository = createRepository()

        assertNull(repository.decreaseStock(UUID.randomUUID(), 1))
    }

    @Test
    fun `deleteById removes product`() = runTest {
        val database = createDatabase()
        val repository = ExposedProductRepository(database)
        val id = repository.create(productRequest())

        val deleted = repository.deleteById(id)

        assertTrue(deleted)
        transaction(database) {
            assertNull(ProductEntity.findById(id))
            assertEquals(0L, ProductEntity.all().count())
        }
    }

    @Test
    fun `deleteById returns false for unknown product`() = runTest {
        val repository = createRepository()

        assertFalse(repository.deleteById(UUID.randomUUID()))
    }

    private fun createRepository(): ExposedProductRepository = ExposedProductRepository(createDatabase())

    private fun createDatabase(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            driver = "org.h2.Driver",
        )
        transaction(database) {
            SchemaUtils.create(AddressTable, ImageTable, SupplierTable, ProductTable)
        }
        return database
    }

    private fun productRequest(
        name: String = "Toaster",
        category: String = "Kitchen",
        price: BigDecimal = BigDecimal("99.99"),
        availableStock: Int = 5,
        lastUpdateDate: LocalDate = LocalDate.of(2024, 1, 1),
        supplierId: UUID? = null,
        imageId: UUID? = null,
    ) = CreateProductRequest(
        name = name,
        category = category,
        price = price,
        availableStock = availableStock,
        lastUpdateDate = lastUpdateDate,
        supplierId = supplierId,
        imageId = imageId,
    )
}
