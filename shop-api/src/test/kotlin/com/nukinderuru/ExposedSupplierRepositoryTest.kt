package com.nukinderuru

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateSupplierRequest
import com.nukinderuru.data.db.dao.AddressEntity
import com.nukinderuru.data.db.dao.AddressTable
import com.nukinderuru.data.db.dao.SupplierEntity
import com.nukinderuru.data.db.dao.SupplierTable
import com.nukinderuru.data.repository.ExposedSupplierRepository
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExposedSupplierRepositoryTest {
    @Test
    fun `create stores trimmed values and findById returns created supplier`() = runTest {
        val repository = createRepository()

        val id = repository.create(
            supplierRequest(
                name = "  Acme Supplies  ",
                phoneNumber = "  +79990000000  ",
                country = "  Russia  ",
                city = "  Moscow  ",
                street = "  Arbat 10  ",
            ),
        )

        val supplier = repository.findById(id)

        assertNotNull(supplier)
        assertEquals("Acme Supplies", supplier.name)
        assertEquals("+79990000000", supplier.phoneNumber)
        assertEquals("Russia", supplier.address.country)
        assertEquals("Moscow", supplier.address.city)
        assertEquals("Arbat 10", supplier.address.street)
    }

    @Test
    fun `findAll returns all suppliers`() = runTest {
        val repository = createRepository()
        repository.create(supplierRequest(name = "Acme"))
        repository.create(supplierRequest(name = "Global"))

        val suppliers = repository.findAll()

        assertEquals(2, suppliers.size)
        assertEquals(setOf("Acme", "Global"), suppliers.map { it.name }.toSet())
    }

    @Test
    fun `updateAddress updates supplier address`() = runTest {
        val repository = createRepository()
        val id = repository.create(supplierRequest())

        val updated = repository.updateAddress(id, AddressRequest("Russia", "Saint Petersburg", "Nevsky Prospect 15"))

        assertNotNull(updated)
        assertEquals("Saint Petersburg", updated.address.city)
        assertEquals("Nevsky Prospect 15", updated.address.street)
    }

    @Test
    fun `deleteById removes supplier and address`() = runTest {
        val database = createDatabase()
        val repository = ExposedSupplierRepository(database)
        val id = repository.create(supplierRequest())
        val addressId = transaction(database) {
            SupplierEntity.findById(id)?.address?.id?.value
        }

        val deleted = repository.deleteById(id)

        assertTrue(deleted)
        transaction(database) {
            assertNull(SupplierEntity.findById(id))
            assertNull(addressId?.let { AddressEntity.findById(it) })
        }
    }

    @Test
    fun `deleteById returns false for unknown supplier`() = runTest {
        val repository = createRepository()

        assertFalse(repository.deleteById(UUID.randomUUID()))
    }

    private fun createRepository() = ExposedSupplierRepository(createDatabase())

    private fun createDatabase(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            driver = "org.h2.Driver",
        )
        transaction(database) {
            SchemaUtils.create(AddressTable, SupplierTable)
        }
        return database
    }

    private fun supplierRequest(
        name: String = "Acme Supplies",
        phoneNumber: String = "+79990000000",
        country: String = "Russia",
        city: String = "Moscow",
        street: String = "Arbat 10",
    ) = CreateSupplierRequest(
        name = name,
        address = AddressRequest(country, city, street),
        phoneNumber = phoneNumber,
    )
}
