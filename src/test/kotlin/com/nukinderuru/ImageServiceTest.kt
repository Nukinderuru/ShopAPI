package com.nukinderuru

import com.nukinderuru.api.dtos.response.CreatedImageResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.repository.ImageRepository
import com.nukinderuru.domain.service.ImageService
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.exception.ValidationException
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImageServiceTest {
    private val repository = FakeImageRepository()
    private val service = ImageService(repository)

    @Test
    fun `addImage returns created image id`() = runTest {
        val response = service.addImage(FakeImageRepository.productId, byteArrayOf(1, 2, 3))

        assertEquals(CreatedImageResponse(FakeImageRepository.imageId), response)
    }

    @Test
    fun `addImage rejects empty image body`() = runTest {
        val exception = assertFailsWith<ValidationException> {
            service.addImage(FakeImageRepository.productId, byteArrayOf())
        }

        assertEquals(ValidationConstants.imageMustNotBeEmpty(), exception.message)
    }

    @Test
    fun `replaceImage throws not found when image missing`() = runTest {
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440991")

        val exception = assertFailsWith<NotFoundException> {
            service.replaceImage(id, byteArrayOf(1))
        }

        assertEquals(ValidationConstants.imageNotFound(id), exception.message)
    }

    @Test
    fun `getImageByProductId returns image bytes`() = runTest {
        val (_, bytes) = service.getImageByProductId(FakeImageRepository.productId)

        assertContentEquals(byteArrayOf(1, 2, 3), bytes)
    }

    @Test
    fun `getImageById returns image bytes`() = runTest {
        val bytes = service.getImageById(FakeImageRepository.imageId)

        assertContentEquals(byteArrayOf(1, 2, 3), bytes)
    }
}

private class FakeImageRepository : ImageRepository {
    override suspend fun createForProduct(productId: UUID, imageBytes: ByteArray): UUID? = imageId.takeIf { productId == Companion.productId }

    override suspend fun replace(imageId: UUID, imageBytes: ByteArray): Boolean = imageId == Companion.imageId

    override suspend fun deleteById(imageId: UUID): Boolean = imageId == Companion.imageId

    override suspend fun findByProductId(productId: UUID): Pair<UUID, ByteArray>? =
        (imageId to byteArrayOf(1, 2, 3)).takeIf { productId == Companion.productId }

    override suspend fun findById(imageId: UUID): ByteArray? =
        byteArrayOf(1, 2, 3).takeIf { imageId == Companion.imageId }

    companion object {
        val productId: UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440400")
        val imageId: UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440401")
    }
}
