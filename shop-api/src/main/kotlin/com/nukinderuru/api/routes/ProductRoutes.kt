package com.nukinderuru.api.routes

import com.nukinderuru.api.dtos.request.CreateProductRequest
import com.nukinderuru.api.dtos.request.DecreaseProductStockRequest
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.service.ProductService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.productRoutes() {
    val productService by inject<ProductService>()

    route("/products") {
        /**
         * @tag Products
         * @description Returns all products with available stock greater than zero.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get("/available") {
            val products = productService.getAllAvailableProducts()
            call.respond(products)
        }

        /**
         * @tag Products
         * @description Returns a single product by its identifier.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Product not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get("/{id}") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseProductUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val product = productService.getProductById(id)
            call.respond(product)
        }

        /**
         * @tag Products
         * @description Creates a new product and returns its identifier.
         * @body application/json CreateProductRequest Product data.
         * @response 400 application/json ErrorResponse Invalid request body.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        post {
            val request = call.receive<CreateProductRequest>()
            val createdProduct = productService.createProduct(request)
            call.response.header(HttpHeaders.Location, "/api/v1/products/${createdProduct.id}")
            call.respond(HttpStatusCode.Created, createdProduct)
        }

        /**
         * @tag Products
         * @description Decreases the available stock of the specified product.
         * @body application/json DecreaseProductStockRequest Amount to decrease.
         * @response 400 application/json ErrorResponse Invalid UUID or request body.
         * @response 404 application/json ErrorResponse Product not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        patch("/{id}/stock") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseProductUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val request = call.receive<DecreaseProductStockRequest>()
            val product = productService.decreaseProductStock(id, request)
            call.respond(product)
        }

        /**
         * @tag Products
         * @description Deletes the product identified by the given UUID.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Product not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        delete("/{id}") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseProductUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            productService.deleteProduct(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun parseProductUuid(rawValue: String): UUID =
    runCatching { UUID.fromString(rawValue) }
        .getOrElse {
            throw IllegalArgumentException(
                ValidationConstants.pathParameterMustBeValidUuid(ValidationConstants.PATH_PARAMETER_ID),
            )
        }