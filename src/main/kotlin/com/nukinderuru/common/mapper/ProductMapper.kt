package com.nukinderuru.common.mapper

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.data.db.dao.ProductEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ProductDraft(
    val name: String,
    val category: String,
    val price: BigDecimal,
    val availableStock: Int,
    val lastUpdateDate: LocalDate,
    val supplierId: UUID?,
    val imageId: UUID?
)

fun CreateProductRequest.toProductDraft(): ProductDraft = ProductDraft(
    name = name.trim(),
    category = category.trim(),
    price = price,
    availableStock = availableStock,
    lastUpdateDate = lastUpdateDate,
    supplierId = supplierId,
    imageId = imageId
)

fun ProductEntity.toResponse(): ProductResponse = ProductResponse(
    id = id.value,
    name = name,
    category = category,
    price = price,
    availableStock = availableStock,
    lastUpdateDate = lastUpdateDate,
    supplierId = supplier?.id?.value,
    imageId = image?.id?.value
)
