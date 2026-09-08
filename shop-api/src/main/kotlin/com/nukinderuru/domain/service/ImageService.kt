package com.nukinderuru.domain.service

import com.nukinderuru.api.dtos.response.CreatedImageResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.data.repository.ImageRepository
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.exception.ValidationException
import java.util.UUID

class ImageService(private val imageRepository: ImageRepository) {
    suspend fun getImageById(imageId: UUID): ByteArray =
        imageRepository.findById(imageId)
            ?: throw NotFoundException(ValidationConstants.imageNotFound(imageId))

    suspend fun getImageByProductId(productId: UUID): Pair<UUID, ByteArray> =
        imageRepository.findByProductId(productId)
            ?: throw NotFoundException(ValidationConstants.imageForProductNotFound(productId))

    suspend fun addImage(productId: UUID, imageBytes: ByteArray): CreatedImageResponse {
        validateImageBytes(imageBytes)
        val imageId = imageRepository.createForProduct(productId, imageBytes)
            ?: throw NotFoundException(ValidationConstants.productNotFound(productId))
        return CreatedImageResponse(imageId)
    }

    suspend fun replaceImage(imageId: UUID, imageBytes: ByteArray) {
        validateImageBytes(imageBytes)
        if (!imageRepository.replace(imageId, imageBytes)) {
            throw NotFoundException(ValidationConstants.imageNotFound(imageId))
        }
    }

    suspend fun deleteImage(imageId: UUID) {
        if (!imageRepository.deleteById(imageId)) {
            throw NotFoundException(ValidationConstants.imageNotFound(imageId))
        }
    }

    private fun validateImageBytes(imageBytes: ByteArray) {
        if (imageBytes.isEmpty()) {
            throw ValidationException(ValidationConstants.imageMustNotBeEmpty())
        }
    }
}
