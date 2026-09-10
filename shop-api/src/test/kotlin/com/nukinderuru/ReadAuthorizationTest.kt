package com.nukinderuru

import com.nukinderuru.api.routes.clientRoutes
import com.nukinderuru.api.routes.imageRoutes
import com.nukinderuru.api.routes.supplierRoutes
import com.nukinderuru.auth.AuthClient
import com.nukinderuru.auth.AuthHttpRequest
import com.nukinderuru.auth.RegisterHttpRequest
import com.nukinderuru.auth.ResetPasswordHttpRequest
import com.nukinderuru.common.config.configureAuthorization
import com.nukinderuru.common.config.configureSerialization
import com.nukinderuru.common.config.configureStatusPages
import com.nukinderuru.domain.service.ClientService
import com.nukinderuru.domain.service.ImageService
import com.nukinderuru.domain.service.SupplierService
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadAuthorizationTest {
    @Test
    fun `get clients rejects missing bearer token`() = testApplication {
        val clientService = mockk<ClientService>(relaxed = true)
        application { clientModule(clientService) }

        val response = client.get("/api/v1/clients")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        coVerify(exactly = 0) { clientService.getAllClients(any(), any()) }
    }

    @Test
    fun `get suppliers rejects missing bearer token`() = testApplication {
        val supplierService = mockk<SupplierService>(relaxed = true)
        application { supplierModule(supplierService) }

        val response = client.get("/api/v1/suppliers")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        coVerify(exactly = 0) { supplierService.getAllSuppliers() }
    }

    @Test
    fun `get product image remains public`() = testApplication {
        val imageService = mockk<ImageService>()
        val productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440555")
        val imageId = UUID.fromString("550e8400-e29b-41d4-a716-446655440666")
        coEvery { imageService.getImageByProductId(productId) } returns (imageId to byteArrayOf(1, 2, 3))
        application { imageModule(imageService) }

        val response = client.get("/api/v1/products/$productId/image")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().isNotEmpty())
    }

    private fun io.ktor.server.application.Application.clientModule(clientService: ClientService) {
        baseModule { single { clientService } }
        routing { route("/api/v1") { clientRoutes() } }
    }

    private fun io.ktor.server.application.Application.supplierModule(supplierService: SupplierService) {
        baseModule { single { supplierService } }
        routing { route("/api/v1") { supplierRoutes() } }
    }

    private fun io.ktor.server.application.Application.imageModule(imageService: ImageService) {
        baseModule { single { imageService } }
        routing { route("/api/v1") { imageRoutes() } }
    }

    private fun io.ktor.server.application.Application.baseModule(extraDefinitions: org.koin.core.module.Module.() -> Unit) {
        configureSerialization()
        configureStatusPages()
        install(Koin) {
            modules(
                module {
                    single<AuthClient> { FakeReadAuthClient() }
                    extraDefinitions()
                },
            )
        }
        configureAuthorization()
    }
}

private class FakeReadAuthClient : AuthClient {
    override suspend fun register(request: RegisterHttpRequest): String = "token"
    override suspend fun authenticate(request: AuthHttpRequest): String = "token"
    override suspend fun resetPassword(request: ResetPasswordHttpRequest): Boolean = true
    override suspend fun validateToken(token: String): Boolean = token == "valid-token"
}
