package com.nukinderuru.data.repository

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.response.ProductResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.common.mapper.toProductDraft
import com.nukinderuru.common.mapper.toResponse
import com.nukinderuru.data.db.dao.ImageEntity
import com.nukinderuru.data.db.dao.ProductEntity
import com.nukinderuru.data.db.dao.ProductTable
import com.nukinderuru.data.db.dao.SupplierEntity
import com.nukinderuru.domain.exception.ValidationException
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.util.UUID

class ExposedProductRepository(private val database: Database) : ProductRepository {
    override suspend fun findAllAvailable(): List<ProductResponse> = dbQuery {
        ProductEntity.all()
            .orderBy(ProductTable.id to SortOrder.ASC)
            .filter { it.availableStock > 0 }
            .map(ProductEntity::toResponse)
    }

    override suspend fun findById(id: UUID): ProductResponse? = dbQuery {
        ProductEntity.findById(id)?.toResponse()
    }

    override suspend fun create(request: CreateProductRequest): UUID = dbQuery {
        val draft = request.toProductDraft()
        val supplier = draft.supplierId?.let { SupplierEntity.findById(it) }
        val image = draft.imageId?.let { ImageEntity.findById(it) }

        ProductEntity.new {
            name = draft.name
            category = draft.category
            price = draft.price
            availableStock = draft.availableStock
            lastUpdateDate = draft.lastUpdateDate
            this.supplier = supplier
            this.image = image
        }.id.value
    }

    override suspend fun decreaseStock(id: UUID, decreaseBy: Int): ProductResponse? = dbQuery {
        val product = ProductEntity.findById(id) ?: return@dbQuery null
        if (decreaseBy > product.availableStock) {
            throw ValidationException(ValidationConstants.productDecreaseExceedsStock(product.availableStock, decreaseBy))
        }
        product.availableStock -= decreaseBy
        product.lastUpdateDate = LocalDate.now()
        product.toResponse()
    }

    override suspend fun deleteById(id: UUID): Boolean = dbQuery {
        val product = ProductEntity.findById(id) ?: return@dbQuery false
        product.delete()
        true
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
