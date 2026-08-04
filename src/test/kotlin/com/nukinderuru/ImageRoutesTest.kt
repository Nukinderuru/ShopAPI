package com.nukinderuru

import com.nukinderuru.api.dtos.response.CreatedImageResponse
import com.nukinderuru.api.routes.imageRoutes
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.service.ImageService
import com.nukinderuru.domain.exception.NotFoundException
import com.nukinderuru.domain.exception.ValidationException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageRoutesTest {
    @Test
    fun `post product image returns 201 and location header`() = testApplication {
        val service = mockk<ImageService>()
        val productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440500")
        val imageId = UUID.fromString("550e8400-e29b-41d4-a716-446655440501")
        coEvery { service.addImage(productId, any()) } returns CreatedImageResponse(imageId)

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.post("/api/v1/products/$productId/image") {
            contentType(ContentType.Application.OctetStream)
            setBody(byteArrayOf(1, 2, 3))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("/api/v1/images/$imageId", response.headers[HttpHeaders.Location])
        coVerify(exactly = 1) { service.addImage(productId, any()) }
    }

    @Test
    fun `get image by id returns octet stream and download header`() = testApplication {
        val service = mockk<ImageService>()
        val imageId = UUID.fromString("550e8400-e29b-41d4-a716-446655440511")
        coEvery { service.getImageById(imageId) } returns byteArrayOf(4, 5, 6)

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/images/$imageId")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.OctetStream.toString(), response.headers[HttpHeaders.ContentType])
        assertTrue(response.headers[HttpHeaders.ContentDisposition]?.contains("image-$imageId.bin") == true)
        assertContentEquals(byteArrayOf(4, 5, 6), response.readRawBytes())
    }

    @Test
    fun `get image by product id returns octet stream`() = testApplication {
        val service = mockk<ImageService>()
        val productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440522")
        val imageId = UUID.fromString("550e8400-e29b-41d4-a716-446655440523")
        coEvery { service.getImageByProductId(productId) } returns (imageId to byteArrayOf(7, 8))

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/products/$productId/image")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContentEquals(byteArrayOf(7, 8), response.readRawBytes())
    }

    @Test
    fun `put image returns 204`() = testApplication {
        val service = mockk<ImageService>()
        val imageId = UUID.fromString("550e8400-e29b-41d4-a716-446655440533")
        coEvery { service.replaceImage(imageId, any()) } returns Unit

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.put("/api/v1/images/$imageId") {
            contentType(ContentType.Application.OctetStream)
            setBody(byteArrayOf(1))
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `delete image returns 204`() = testApplication {
        val service = mockk<ImageService>()
        val imageId = UUID.fromString("550e8400-e29b-41d4-a716-446655440544")
        coEvery { service.deleteImage(imageId) } returns Unit

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.delete("/api/v1/images/$imageId")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `post product image returns 400 for empty body`() = testApplication {
        val service = mockk<ImageService>()
        val productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440555")
        coEvery { service.addImage(productId, any()) } throws ValidationException(ValidationConstants.imageMustNotBeEmpty())

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.post("/api/v1/products/$productId/image") {
            contentType(ContentType.Application.OctetStream)
            setBody(byteArrayOf())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.imageMustNotBeEmpty()))
    }

    @Test
    fun `get image by id returns 404 when service throws not found`() = testApplication {
        val service = mockk<ImageService>()
        val imageId = UUID.fromString("550e8400-e29b-41d4-a716-446655440566")
        coEvery { service.getImageById(imageId) } throws NotFoundException(ValidationConstants.imageNotFound(imageId))

        environment { config = MapApplicationConfig() }
        application { testModule(service) }

        val response = client.get("/api/v1/images/$imageId")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains(ValidationConstants.imageNotFound(imageId)))
    }

    private fun io.ktor.server.application.Application.testModule(service: ImageService) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(module { single { service } })
        }
        routing {
            route("/api/v1") {
                imageRoutes()
            }
        }
    }
}
