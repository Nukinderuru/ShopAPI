package com.nukinderuru.api.routes

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateSupplierRequest
import com.nukinderuru.auth.requireAuthorization
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.service.SupplierService
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


fun Route.supplierRoutes() {
    val supplierService by inject<SupplierService>()

    route("/suppliers") {
        /**
         * @tag Suppliers
         * @description Returns all suppliers.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get {
            call.respond(supplierService.getAllSuppliers())
        }.requireAuthorization()

        /**
         * @tag Suppliers
         * @description Returns a single supplier by its identifier.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Supplier not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get("/{id}") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseSupplierUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            call.respond(supplierService.getSupplierById(id))
        }.requireAuthorization()

        /**
         * @tag Suppliers
         * @description Creates a new supplier and returns its identifier.
         * @body application/json CreateSupplierRequest Supplier data.
         * @response 400 application/json ErrorResponse Invalid request body.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        post {
            val request = call.receive<CreateSupplierRequest>()
            val createdSupplier = supplierService.createSupplier(request)
            call.response.header(HttpHeaders.Location, "/api/v1/suppliers/${createdSupplier.id}")
            call.respond(HttpStatusCode.Created, createdSupplier)
        }.requireAuthorization()

        /**
         * @tag Suppliers
         * @description Updates the address of the specified supplier.
         * @body application/json AddressRequest New address.
         * @response 400 application/json ErrorResponse Invalid UUID or request body.
         * @response 404 application/json ErrorResponse Supplier not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        patch("/{id}/address") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseSupplierUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val request = call.receive<AddressRequest>()
            call.respond(supplierService.changeSupplierAddress(id, request))
        }.requireAuthorization()

        /**
         * @tag Suppliers
         * @description Deletes the supplier identified by the given UUID.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Supplier not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        delete("/{id}") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseSupplierUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            supplierService.deleteSupplier(id)
            call.respond(HttpStatusCode.NoContent)
        }.requireAuthorization()
    }
}

private fun parseSupplierUuid(rawValue: String): UUID =
    runCatching { UUID.fromString(rawValue) }
        .getOrElse {
            throw IllegalArgumentException(
                ValidationConstants.pathParameterMustBeValidUuid(ValidationConstants.PATH_PARAMETER_ID),
            )
        }
