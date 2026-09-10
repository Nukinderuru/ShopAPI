package com.nukinderuru

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateClientRequest
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.db.dao.AddressEntity
import com.nukinderuru.data.db.dao.AddressTable
import com.nukinderuru.data.db.dao.ClientEntity
import com.nukinderuru.data.db.dao.ClientTable
import com.nukinderuru.data.db.dao.Gender
import com.nukinderuru.data.repository.ExposedClientRepository
import com.nukinderuru.domain.exception.ValidationException
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ExposedClientRepositoryTest {

    @Test
    fun `create stores trimmed values and findById returns created client`() = runTest {
        val repository = createRepository()

        val id = repository.create(
            clientRequest(
                clientName = "  Jane  ",
                clientSurname = "  Doe  ",
                country = "  Russia  ",
                city = "  Moscow  ",
                street = "  Arbat 10  ",
            ),
        )

        val client = repository.findById(id)

        assertNotNull(client)
        assertEquals(id, client.id)
        assertEquals("Jane", client.clientName)
        assertEquals("Doe", client.clientSurname)
        assertEquals("Russia", client.address.country)
        assertEquals("Moscow", client.address.city)
        assertEquals("Arbat 10", client.address.street)
    }

    @Test
    fun `findById returns null for unknown id`() = runTest {
        val repository = createRepository()

        val client = repository.findById(UUID.randomUUID())

        assertNull(client)
    }

    @Test
    fun `findByName returns trimmed matches in id order`() = runTest {
        val repository = createRepository()
        repository.create(clientRequest(clientName = "John", clientSurname = "Doe", city = "Moscow"))
        repository.create(clientRequest(clientName = "John", clientSurname = "Doe", city = "Kazan"))
        repository.create(clientRequest(clientName = "John", clientSurname = "Smith", city = "Perm"))

        val clients = repository.findByName("  John  ", "  Doe  ")

        assertEquals(2, clients.size)
        assertEquals(setOf("Moscow", "Kazan"), clients.map { it.address.city }.toSet())
    }

    @Test
    fun `findAll without pagination returns all clients in id order`() = runTest {
        val repository = createRepository()
        repository.create(clientRequest(clientName = "Alice"))
        repository.create(clientRequest(clientName = "Bob"))

        val clients = repository.findAll(limit = null, offset = null)

        assertEquals(2, clients.size)
        assertEquals(setOf("Alice", "Bob"), clients.map { it.clientName }.toSet())
    }

    @Test
    fun `findAll with limit and offset returns requested window`() = runTest {
        val repository = createRepository()
        repository.create(clientRequest(clientName = "First"))
        repository.create(clientRequest(clientName = "Second"))
        repository.create(clientRequest(clientName = "Third"))

        val allClients = repository.findAll(limit = null, offset = null)
        val clients = repository.findAll(limit = 2, offset = 1)

        assertEquals(allClients.drop(1).take(2).map { it.id }, clients.map { it.id })
    }

    @Test
    fun `findAll with limit only returns leading slice`() = runTest {
        val repository = createRepository()
        repository.create(clientRequest(clientName = "First"))
        repository.create(clientRequest(clientName = "Second"))
        repository.create(clientRequest(clientName = "Third"))

        val allClients = repository.findAll(limit = null, offset = null)
        val clients = repository.findAll(limit = 2, offset = null)

        assertEquals(allClients.take(2).map { it.id }, clients.map { it.id })
    }

    @Test
    fun `findAll with offset only skips leading clients`() = runTest {
        val repository = createRepository()
        repository.create(clientRequest(clientName = "First"))
        repository.create(clientRequest(clientName = "Second"))
        repository.create(clientRequest(clientName = "Third"))

        val allClients = repository.findAll(limit = null, offset = null)
        val clients = repository.findAll(limit = null, offset = 1)

        assertEquals(allClients.drop(1).map { it.id }, clients.map { it.id })
    }

    @Test
    fun `findAll throws validation exception for too large offset`() = runTest {
        val repository = createRepository()

        val exception = assertFailsWith<ValidationException> {
            repository.findAll(limit = null, offset = Int.MAX_VALUE.toLong() + 1)
        }

        assertEquals(
            ValidationConstants.queryParameterTooLarge(ValidationConstants.QUERY_PARAMETER_OFFSET),
            exception.message,
        )
    }

    @Test
    fun `updateAddress updates stored address and returns updated client`() = runTest {
        val repository = createRepository()
        val id = repository.create(clientRequest(city = "Moscow", street = "Arbat 10"))

        val updated = repository.updateAddress(
            id,
            AddressRequest(
                country = "  Russia  ",
                city = "  Saint Petersburg  ",
                street = "  Nevsky Prospect 15  ",
            ),
        )

        assertNotNull(updated)
        assertEquals("Russia", updated.address.country)
        assertEquals("Saint Petersburg", updated.address.city)
        assertEquals("Nevsky Prospect 15", updated.address.street)
    }

    @Test
    fun `updateAddress returns null for unknown client`() = runTest {
        val repository = createRepository()

        val updated = repository.updateAddress(UUID.randomUUID(), AddressRequest("Russia", "Moscow", "Arbat 10"))

        assertNull(updated)
    }

    @Test
    fun `deleteById removes client and its address`() = runTest {
        val database = createDatabase()
        val repository = ExposedClientRepository(database)
        val id = repository.create(clientRequest())
        val addressId = transaction(database) {
            ClientEntity.findById(id)?.address?.id?.value
        }

        val deleted = repository.deleteById(id)

        assertTrue(deleted)
        assertNull(repository.findById(id))
        transaction(database) {
            assertNull(ClientEntity.findById(id))
            assertNull(addressId?.let { AddressEntity.findById(it) })
            assertEquals(0L, ClientEntity.all().count())
            assertEquals(0L, AddressEntity.all().count())
        }
    }

    @Test
    fun `deleteById returns false for unknown client`() = runTest {
        val repository = createRepository()

        assertFalse(repository.deleteById(UUID.randomUUID()))
    }

    private fun createRepository(): ExposedClientRepository = ExposedClientRepository(createDatabase())

    private fun createDatabase(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            driver = "org.h2.Driver",
        )
        transaction(database) {
            SchemaUtils.create(AddressTable, ClientTable)
        }
        return database
    }

    private fun clientRequest(
        clientName: String = "Jane",
        clientSurname: String = "Doe",
        birthday: LocalDate = LocalDate.of(1994, 2, 10),
        gender: Gender = Gender.FEMALE,
        registrationDate: LocalDate = LocalDate.of(2024, 1, 1),
        country: String = "Russia",
        city: String = "Moscow",
        street: String = "Arbat 10",
    ): CreateClientRequest = CreateClientRequest(
        clientName = clientName,
        clientSurname = clientSurname,
        birthday = birthday,
        gender = gender,
        registrationDate = registrationDate,
        address = AddressRequest(
            country = country,
            city = city,
            street = street,
        ),
    )
}
