package com.nukinderuru.common.constants

import java.util.UUID

object ValidationConstants {
    const val BAD_REQUEST = "Bad request"
    const val NOT_FOUND = "Not found"
    const val INTERNAL_SERVER_ERROR = "Internal server error"

    const val VALIDATION_FAILED = "Validation failed"
    const val INVALID_REQUEST = "Invalid request"
    const val INVALID_REQUEST_BODY = "Invalid request body"
    const val ENTITY_NOT_FOUND = "Entity not found"
    const val UNEXPECTED_SERVER_ERROR = "Unexpected server error"

    const val QUERY_PARAMETER_LIMIT = "limit"
    const val QUERY_PARAMETER_OFFSET = "offset"
    const val QUERY_PARAMETER_FIRST_NAME = "firstName"
    const val QUERY_PARAMETER_LAST_NAME = "lastName"
    const val PATH_PARAMETER_ID = "id"

    const val FIELD_CLIENT_NAME = "clientName"
    const val FIELD_CLIENT_SURNAME = "clientSurname"
    const val FIELD_BIRTHDAY = "birthday"
    const val FIELD_REGISTRATION_DATE = "registrationDate"
    const val FIELD_COUNTRY = "country"
    const val FIELD_CITY = "city"
    const val FIELD_STREET = "street"
    const val FIELD_PRODUCT_NAME = "name"
    const val FIELD_PRODUCT_CATEGORY = "category"
    const val FIELD_PRODUCT_PRICE = "price"
    const val FIELD_AVAILABLE_STOCK = "availableStock"
    const val FIELD_LAST_UPDATE_DATE = "lastUpdateDate"
    const val FIELD_DECREASE_BY = "decreaseBy"
    const val FIELD_SUPPLIER_NAME = "name"
    const val FIELD_PHONE_NUMBER = "phoneNumber"

    const val ENTITY_CLIENT = "Client"
    const val ENTITY_PRODUCT = "Product"
    const val ENTITY_SUPPLIER = "Supplier"
    const val ENTITY_IMAGE = "Image"

    fun queryParameterMustBeInteger(parameter: String): String =
        "Query parameter '$parameter' must be an integer"

    fun queryParameterRequired(parameter: String): String =
        "Query parameter '$parameter' is required"

    fun queryParameterMustBeGreaterThanZero(parameter: String): String =
        "Query parameter '$parameter' must be greater than 0"

    fun queryParameterMustBeGreaterThanOrEqualToZero(parameter: String): String =
        "Query parameter '$parameter' must be greater than or equal to 0"

    fun queryParameterTooLarge(parameter: String): String =
        "Query parameter '$parameter' is too large"

    fun fieldMustBeGreaterThanZero(fieldName: String): String =
        "'$fieldName' must be greater than 0"

    fun fieldMustBeGreaterThanOrEqualToZero(fieldName: String): String =
        "'$fieldName' must be greater than or equal to 0"

    fun fieldMustNotBeBlank(fieldName: String): String =
        "'$fieldName' must not be blank"

    fun fieldMustNotBeInFuture(fieldName: String): String =
        "'$fieldName' must not be in the future"

    fun fieldCannotBeEarlierThan(fieldName: String, otherFieldName: String): String =
        "'$fieldName' cannot be earlier than '$otherFieldName'"

    fun pathParameterRequired(parameter: String): String =
        "Path parameter '$parameter' is required"

    fun pathParameterMustBeValidUuid(parameter: String): String =
        "Path parameter '$parameter' must be a valid UUID"

    fun clientNotFound(id: UUID): String =
        "$ENTITY_CLIENT with id=$id was not found"

    fun productNotFound(id: UUID): String =
        "$ENTITY_PRODUCT with id=$id was not found"

    fun supplierNotFound(id: UUID): String =
        "$ENTITY_SUPPLIER with id=$id was not found"

    fun imageNotFound(id: UUID): String =
        "$ENTITY_IMAGE with id=$id was not found"

    fun imageForProductNotFound(productId: UUID): String =
        "$ENTITY_IMAGE for product id=$productId was not found"

    fun imageMustNotBeEmpty(): String =
        "Image body must not be empty"

    fun productDecreaseExceedsStock(availableStock: Int, decreaseBy: Int): String =
        "Cannot decrease product stock by $decreaseBy when only $availableStock items are available"
}
