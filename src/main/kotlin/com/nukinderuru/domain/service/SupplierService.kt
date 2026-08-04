package com.nukinderuru.domain.service

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateSupplierRequest
import com.nukinderuru.api.dtos.response.CreatedSupplierResponse
import com.nukinderuru.api.dtos.response.SupplierResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.repository.SupplierRepository
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.exception.ValidationException
import java.util.UUID

class SupplierService(private val supplierRepository: SupplierRepository) {
    suspend fun getAllSuppliers(): List<SupplierResponse> = supplierRepository.findAll()

    suspend fun getSupplierById(id: UUID): SupplierResponse =
        supplierRepository.findById(id) ?: throw NotFoundException(ValidationConstants.supplierNotFound(id))

    suspend fun createSupplier(request: CreateSupplierRequest): CreatedSupplierResponse {
        validateCreateRequest(request)
        return CreatedSupplierResponse(supplierRepository.create(request))
    }

    suspend fun changeSupplierAddress(id: UUID, addressRequest: AddressRequest): SupplierResponse {
        validateAddress(addressRequest)
        return supplierRepository.updateAddress(id, addressRequest)
            ?: throw NotFoundException(ValidationConstants.supplierNotFound(id))
    }

    suspend fun deleteSupplier(id: UUID) {
        if (!supplierRepository.deleteById(id)) {
            throw NotFoundException(ValidationConstants.supplierNotFound(id))
        }
    }

    private fun validateCreateRequest(request: CreateSupplierRequest) {
        validateRequiredText(request.name, ValidationConstants.FIELD_SUPPLIER_NAME)
        validateAddress(request.address)
        validateRequiredText(request.phoneNumber, ValidationConstants.FIELD_PHONE_NUMBER)
    }

    private fun validateAddress(addressRequest: AddressRequest) {
        validateRequiredText(addressRequest.country, ValidationConstants.FIELD_COUNTRY)
        validateRequiredText(addressRequest.city, ValidationConstants.FIELD_CITY)
        validateRequiredText(addressRequest.street, ValidationConstants.FIELD_STREET)
    }

    private fun validateRequiredText(value: String, fieldName: String) {
        if (value.isBlank()) {
            throw ValidationException(ValidationConstants.fieldMustNotBeBlank(fieldName))
        }
    }
}
