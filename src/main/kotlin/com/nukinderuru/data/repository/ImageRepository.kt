package com.nukinderuru.data.repository

import java.util.UUID

interface ImageRepository {
    suspend fun findById(imageId: UUID): ByteArray?

    suspend fun findByProductId(productId: UUID): Pair<UUID, ByteArray>?

    suspend fun createForProduct(productId: UUID, imageBytes: ByteArray): UUID?

    suspend fun replace(imageId: UUID, imageBytes: ByteArray): Boolean

    suspend fun deleteById(imageId: UUID): Boolean
}
