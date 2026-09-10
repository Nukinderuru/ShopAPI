package com.nukinderuru.common.config

import com.fasterxml.jackson.core.JacksonException
import com.nukinderuru.api.dtos.response.ErrorResponse
import com.nukinderuru.auth.UnauthorizedException
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.domain.exception.NotFoundException
import io.ktor.http.HttpStatusCode
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ApplicationErrors")

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            logger.warn("Validation error on {} {}: {}", call.request.httpMethod.value, call.request.uri, cause.message)
            call.respond(
                HttpStatusCode.BadRequest, ErrorResponse(
                    ValidationConstants.BAD_REQUEST,
                    HttpStatusCode.BadRequest.value,
                    cause.message ?: ValidationConstants.VALIDATION_FAILED
                )
            )
        }
        exception<JacksonException> { call, _ ->
            logger.warn("Jackson error on {} {}", call.request.httpMethod.value, call.request.uri)
            call.respond(
                HttpStatusCode.BadRequest, ErrorResponse(
                    ValidationConstants.BAD_REQUEST,
                    HttpStatusCode.BadRequest.value,
                    ValidationConstants.INVALID_REQUEST_BODY
                )
            )
        }
        exception<ContentTransformationException> { call, _ ->
            logger.warn("Content transformation error on {} {}", call.request.httpMethod.value, call.request.uri)
            call.respond(
                HttpStatusCode.BadRequest, ErrorResponse(
                    ValidationConstants.BAD_REQUEST,
                    HttpStatusCode.BadRequest.value,
                    ValidationConstants.INVALID_REQUEST_BODY
                )
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            logger.warn("Illegal argument on {} {}: {}", call.request.httpMethod.value, call.request.uri, cause.message)
            call.respond(
                HttpStatusCode.BadRequest, ErrorResponse(
                    ValidationConstants.BAD_REQUEST,
                    HttpStatusCode.BadRequest.value,
                    cause.message ?: ValidationConstants.INVALID_REQUEST
                )
            )
        }
        exception<NotFoundException> { call, cause ->
            logger.info("Not found on {} {}: {}", call.request.httpMethod.value, call.request.uri, cause.message)
            call.respond(
                HttpStatusCode.NotFound, ErrorResponse(
                    ValidationConstants.NOT_FOUND,
                    HttpStatusCode.NotFound.value,
                    cause.message ?: ValidationConstants.ENTITY_NOT_FOUND
                )
            )
        }
        exception<UnauthorizedException> { call, cause ->
            logger.warn("Unauthorized request on {} {}: {}", call.request.httpMethod.value, call.request.uri, cause.message)
            call.respond(
                HttpStatusCode.Unauthorized, ErrorResponse(
                    "Unauthorized",
                    HttpStatusCode.Unauthorized.value,
                    cause.message ?: "Unauthorized",
                )
            )
        }
        exception<StatusRuntimeException> { call, cause ->
            call.respondGrpcStatus(cause.status)
        }
        exception<StatusException> { call, cause ->
            call.respondGrpcStatus(cause.status)
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled error on {} {}", call.request.httpMethod.value, call.request.uri, cause)
            call.respond(
                HttpStatusCode.InternalServerError, ErrorResponse(
                    ValidationConstants.INTERNAL_SERVER_ERROR,
                    HttpStatusCode.InternalServerError.value,
                    ValidationConstants.UNEXPECTED_SERVER_ERROR
                )
            )
        }
    }
}

private suspend fun ApplicationCall.respondGrpcStatus(status: Status) {
    val statusCode = when (status.code) {
        Status.Code.UNAUTHENTICATED -> HttpStatusCode.Unauthorized
        Status.Code.ALREADY_EXISTS -> HttpStatusCode.Conflict
        Status.Code.INVALID_ARGUMENT -> HttpStatusCode.BadRequest
        else -> HttpStatusCode.InternalServerError
    }
    val error = statusCode.description
    respond(
        statusCode, ErrorResponse(
            error,
            statusCode.value,
            status.description ?: error,
        )
    )
}
