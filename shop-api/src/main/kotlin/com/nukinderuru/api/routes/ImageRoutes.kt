package com.nukinderuru.api.routes

import com.nukinderuru.auth.requireAuthorization
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.service.ImageService
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.imageRoutes() {
    val imageService by inject<ImageService>()

    route("/images") {
        /**
         * @tag Images
         * @description Returns the image identified by the given UUID.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Image not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get("/{id}") {
            val imageId = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseImageUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val imageBytes = imageService.getImageById(imageId)
            call.respondImage(imageId, imageBytes)
        }

        /**
         * @tag Images
         * @description Replaces the binary content of the specified image.
         * @body application/octet-stream ByteArray Raw image bytes.
         * @response 400 application/json ErrorResponse Invalid UUID or empty image body.
         * @response 404 application/json ErrorResponse Image not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        put("/{id}") {
            val imageId = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseImageUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val imageBytes = call.receive<ByteArray>()
            imageService.replaceImage(imageId, imageBytes)
            call.respond(HttpStatusCode.NoContent)
        }.requireAuthorization()

        /**
         * @tag Images
         * @description Deletes the image identified by the given UUID.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Image not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        delete("/{id}") {
            val imageId = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseImageUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            imageService.deleteImage(imageId)
            call.respond(HttpStatusCode.NoContent)
        }.requireAuthorization()
    }

    route("/products") {
        /**
         * @tag Images
         * @description Returns the image associated with the specified product.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Image for product not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get("/{id}/image") {
            val productId = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseImageUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val (imageId, imageBytes) = imageService.getImageByProductId(productId)
            call.respondImage(imageId, imageBytes)
        }

        /**
         * @tag Images
         * @description Adds an image for the specified product.
         * @body application/octet-stream ByteArray Raw image bytes.
         * @response 400 application/json ErrorResponse Invalid UUID or empty image body.
         * @response 404 application/json ErrorResponse Product not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        post("/{id}/image") {
            val productId = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseImageUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val imageBytes = call.receive<ByteArray>()
            val createdImage = imageService.addImage(productId, imageBytes)
            call.response.header(HttpHeaders.Location, "/api/v1/images/${createdImage.id}")
            call.respond(HttpStatusCode.Created, createdImage)
        }.requireAuthorization()
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondImage(imageId: UUID, imageBytes: ByteArray) {
    response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "image-$imageId.bin").toString(),
    )
    respondBytes(imageBytes, ContentType.Application.OctetStream)
}

private fun parseImageUuid(rawValue: String): UUID =
    runCatching { UUID.fromString(rawValue) }
        .getOrElse {
            throw IllegalArgumentException(
                ValidationConstants.pathParameterMustBeValidUuid(ValidationConstants.PATH_PARAMETER_ID),
            )
        }
