package com.nukinderuru.common.config

import com.fasterxml.jackson.core.JacksonException
import com.nukinderuru.api.dtos.response.ErrorResponse
import com.nukinderuru.common.constants.ValidationConstants
import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.domain.exception.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
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
