package com.nukinderuru.common.mapper

import com.nukinderuru.api.dtos.request.CreateSupplierRequest
import com.nukinderuru.api.dtos.response.SupplierResponse
import com.nukinderuru.data.db.dao.SupplierEntity

data class SupplierDraft(
    val name: String,
    val phoneNumber: String,
)

fun CreateSupplierRequest.toSupplierDraft(): SupplierDraft = SupplierDraft(
    name = name.trim(),
    phoneNumber = phoneNumber.trim(),
)

fun SupplierEntity.toResponse(): SupplierResponse = SupplierResponse(
    id = id.value,
    name = name,
    address = address.toResponse(),
    phoneNumber = phoneNumber,
)
