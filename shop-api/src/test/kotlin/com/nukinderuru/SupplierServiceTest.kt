package com.nukinderuru

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateSupplierRequest
import com.nukinderuru.api.dtos.response.CreatedSupplierResponse
import com.nukinderuru.api.dtos.response.SupplierResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.repository.SupplierRepository
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.domain.service.SupplierService
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SupplierServiceTest {
    private val repository = FakeSupplierRepository()
    private val service = SupplierService(repository)

    @Test
    fun `createSupplier returns created supplier id`() = runTest {
        val response = service.createSupplier(validRequest())

        assertEquals(CreatedSupplierResponse(FakeSupplierRepository.createdSupplierId), response)
    }

    @Test
    fun `createSupplier rejects blank name`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.createSupplier(validRequest().copy(name = "   "))
        }

        assertEquals(ValidationConstants.fieldMustNotBeBlank(ValidationConstants.FIELD_SUPPLIER_NAME), exception.message)
    }

    @Test
    fun `getSupplierById throws not found when missing`() = runTest {
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440999")

        val exception = assertFailsWith<NotFoundException> {
            service.getSupplierById(id)
        }

        assertEquals(ValidationConstants.supplierNotFound(id), exception.message)
    }

    @Test
    fun `changeSupplierAddress throws not found when missing`() = runTest {
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440998")

        val exception = assertFailsWith<NotFoundException> {
            service.changeSupplierAddress(id, AddressRequest("Russia", "Moscow", "Arbat 10"))
        }

        assertEquals(ValidationConstants.supplierNotFound(id), exception.message)
    }

    @Test
    fun `deleteSupplier throws not found when missing`() = runTest {
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440997")

        val exception = assertFailsWith<NotFoundException> {
            service.deleteSupplier(id)
        }

        assertEquals(ValidationConstants.supplierNotFound(id), exception.message)
    }

    private fun validRequest() = CreateSupplierRequest(
        name = "Acme Supplies",
        address = AddressRequest("Russia", "Moscow", "Arbat 10"),
        phoneNumber = "+79990000000",
    )
}

private class FakeSupplierRepository : SupplierRepository {
    override suspend fun create(request: CreateSupplierRequest): UUID = createdSupplierId

    override suspend fun updateAddress(id: UUID, addressRequest: AddressRequest): SupplierResponse? =
        existingSupplier.takeIf { it.id == id }

    override suspend fun deleteById(id: UUID): Boolean = id == createdSupplierId

    override suspend fun findAll(): List<SupplierResponse> = listOf(existingSupplier)

    override suspend fun findById(id: UUID): SupplierResponse? = existingSupplier.takeIf { it.id == id }

    companion object {
        val createdSupplierId: UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440300")
        val existingSupplier = SupplierResponse(
            id = createdSupplierId,
            name = "Acme Supplies",
            address = com.nukinderuru.api.dtos.response.AddressResponse(
                id = UUID.fromString("550e8400-e29b-41d4-a716-446655440301"),
                country = "Russia",
                city = "Moscow",
                street = "Arbat 10",
            ),
            phoneNumber = "+79990000000",
        )
    }
}
