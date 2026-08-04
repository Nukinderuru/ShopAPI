package com.nukinderuru.api.routes

import com.nukinderuru.api.dtos.request.AddressRequest
import com.nukinderuru.api.dtos.request.CreateClientRequest
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.service.ClientService
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

fun Route.clientRoutes() {
    val clientService by inject<ClientService>()

    route("/clients") {
        /**
         * @tag Clients
         * @description Returns all clients or a paginated subset when limit and offset are provided.
         * @response 400 application/json ErrorResponse Invalid query parameter values.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get {
            val rawLimit = call.request.queryParameters[ValidationConstants.QUERY_PARAMETER_LIMIT]
            val rawOffset = call.request.queryParameters[ValidationConstants.QUERY_PARAMETER_OFFSET]
            val limit = rawLimit?.toIntOrNull()
                ?: if (rawLimit != null) throw IllegalArgumentException(
                    ValidationConstants.queryParameterMustBeInteger(ValidationConstants.QUERY_PARAMETER_LIMIT),
                ) else null
            val offset = rawOffset?.toLongOrNull()
                ?: if (rawOffset != null) throw IllegalArgumentException(
                    ValidationConstants.queryParameterMustBeInteger(ValidationConstants.QUERY_PARAMETER_OFFSET),
                ) else null
            val clients = clientService.getAllClients(limit, offset)
            call.respond(clients)
        }

        /**
         * @tag Clients
         * @description Creates a new client and returns its identifier.
         * @body application/json CreateClientRequest Client data.
         * @response 201 application/json CreatedClientResponse Created client.
         * @response 400 application/json ErrorResponse Invalid request body.
         */
        post {
            val request = call.receive<CreateClientRequest>()
            val createdClient = clientService.createClient(request)
            call.response.header(HttpHeaders.Location, "/api/v1/clients/${createdClient.id}")
            call.respond(HttpStatusCode.Created, createdClient)
        }

        /**
         * @tag Clients
         * @description Returns all clients matching the provided first and last name.
         * @response 400 application/json ErrorResponse Missing or invalid query parameters.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get("/search") {
            val firstName = call.request.queryParameters[ValidationConstants.QUERY_PARAMETER_FIRST_NAME]
                ?: throw IllegalArgumentException(
                    ValidationConstants.queryParameterRequired(ValidationConstants.QUERY_PARAMETER_FIRST_NAME),
                )
            val lastName = call.request.queryParameters[ValidationConstants.QUERY_PARAMETER_LAST_NAME]
                ?: throw IllegalArgumentException(
                    ValidationConstants.queryParameterRequired(ValidationConstants.QUERY_PARAMETER_LAST_NAME),
                )
            val clients = clientService.getClientsByName(firstName, lastName)
            call.respond(clients)
        }

        /**
         * @tag Clients
         * @description Returns a single client by its identifier.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Client not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        get("/{id}") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val client = clientService.getClientById(id)
            call.respond(client)
        }

        /**
         * @tag Clients
         * @description Deletes the client identified by the given UUID.
         * @response 400 application/json ErrorResponse Invalid UUID in path.
         * @response 404 application/json ErrorResponse Client not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        delete("/{id}") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            clientService.deleteClient(id)
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * @tag Clients
         * @description Updates the address of the specified client.
         * @body application/json AddressRequest New address.
         * @response 400 application/json ErrorResponse Invalid UUID or request body.
         * @response 404 application/json ErrorResponse Client not found.
         * @response 500 application/json ErrorResponse Unexpected server error.
         */
        patch("/{id}/address") {
            val id = call.parameters[ValidationConstants.PATH_PARAMETER_ID]?.let(::parseUuid)
                ?: throw IllegalArgumentException(ValidationConstants.pathParameterRequired(ValidationConstants.PATH_PARAMETER_ID))
            val request = call.receive<AddressRequest>()
            val updatedClient = clientService.changeClientAddress(id, request)
            call.respond(updatedClient)
        }
    }
}

private fun parseUuid(rawValue: String): UUID =
    runCatching { UUID.fromString(rawValue) }
        .getOrElse {
            throw IllegalArgumentException(
                ValidationConstants.pathParameterMustBeValidUuid(ValidationConstants.PATH_PARAMETER_ID),
            )
        }
