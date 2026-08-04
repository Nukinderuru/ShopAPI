package com.nukinderuru.data.repository

import com.nukinderuru.data.db.dao.ImageEntity
import com.nukinderuru.data.db.dao.ProductEntity
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ExposedImageRepository(private val database: Database) : ImageRepository {
    override suspend fun findById(imageId: UUID): ByteArray? = dbQuery {
        ImageEntity.findById(imageId)?.image
    }

    override suspend fun findByProductId(productId: UUID): Pair<UUID, ByteArray>? = dbQuery {
        val product = ProductEntity.findById(productId) ?: return@dbQuery null
        val image = product.image ?: return@dbQuery null
        image.id.value to image.image
    }

    override suspend fun createForProduct(productId: UUID, imageBytes: ByteArray): UUID? = dbQuery {
        val product = ProductEntity.findById(productId) ?: return@dbQuery null
        val previousImage = product.image
        val image = ImageEntity.new {
            image = imageBytes
        }
        product.image = image
        previousImage?.delete()
        image.id.value
    }

    override suspend fun replace(imageId: UUID, imageBytes: ByteArray): Boolean = dbQuery {
        val image = ImageEntity.findById(imageId) ?: return@dbQuery false
        image.image = imageBytes
        true
    }

    override suspend fun deleteById(imageId: UUID): Boolean = dbQuery {
        val image = ImageEntity.findById(imageId) ?: return@dbQuery false
        ProductEntity.all().firstOrNull { it.image?.id?.value == imageId }?.image = null
        image.delete()
        true
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
