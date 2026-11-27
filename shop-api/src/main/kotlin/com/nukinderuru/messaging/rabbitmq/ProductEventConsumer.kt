package com.nukinderuru.messaging.rabbitmq

import com.nukinderuru.domain.exception.ValidationException
import com.nukinderuru.domain.service.ProductEventProcessingResult
import com.nukinderuru.domain.service.ProductEventService
import com.nukinderuru.events.ProductChangeEvent
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.SQLException
import java.sql.SQLRecoverableException
import java.sql.SQLTransientException
import java.util.concurrent.TimeoutException
import kotlin.collections.mapValues
import kotlin.collections.orEmpty

data class RabbitDelivery(
    val envelope: Envelope,
    val properties: AMQP.BasicProperties,
    val body: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RabbitDelivery

        if (envelope != other.envelope) return false
        if (properties != other.properties) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = envelope.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}

class ConsumerChannelRestartRequired(cause: Throwable) : RuntimeException(cause)

class ProductEventConsumer(
    private val service: ProductEventService,
    private val publisher: RabbitMessagePublisher,
    private val retryPolicy: RabbitRetryPolicy = RabbitRetryPolicy(),
    private val json: Json = Json { ignoreUnknownKeys = false }
) {
    suspend fun handle(delivery: RabbitDelivery, consumerChannel: Channel) {
        val deliveryTag = delivery.envelope.deliveryTag
        val headers = delivery.properties.headers.orEmpty().mapValues { it.value }
        val event = try {
            json.decodeFromString<ProductChangeEvent>(delivery.body.decodeToString())
        } catch (exception: SerializationException) {
            logger.warn("Rejecting malformed product event", exception)
            consumerChannel.basicReject(deliveryTag, false)
            return
        } catch (exception: IllegalArgumentException) {
            logger.warn("Rejecting invalid product event", exception)
            consumerChannel.basicReject(deliveryTag, false)
            return
        }

        try {
            when (service.process(event)) {
                is ProductEventProcessingResult.Applied -> Unit
                ProductEventProcessingResult.Duplicate -> logger.warn(
                    "Acknowledging duplicate product event {}",
                    event.eventId
                )
                ProductEventProcessingResult.NotFound -> logger.warn(
                    "Acknowledging product event {} for missing product",
                    event.eventId
                )
                ProductEventProcessingResult.Stale -> logger.warn(
                    "Acknowledging stale product event {}",
                    event.eventId
                )
            }
        } catch (exception: ValidationException) {
            logger.warn("Rejecting invalid product event {}: {}", event.eventId, exception.message)
            consumerChannel.basicReject(deliveryTag, false)
            return
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            handleFailure(delivery, event, headers, exception, consumerChannel)
            return
        }
        consumerChannel.basicAck(deliveryTag, false)
    }

    private fun handleFailure(
        delivery: RabbitDelivery,
        event: ProductChangeEvent,
        headers: Map<String, Any>,
        exception: Throwable,
        consumerChannel: Channel
    ) {
        val attempt = retryPolicy.attempt(headers)
        if (isTransient(exception)) {
            val nextAttempt = retryPolicy.nextAttempt(attempt)
            if (nextAttempt != null) {
                publishRetryOrRestart(delivery, headers, nextAttempt, consumerChannel)
            } else {
                publishDeadLetterOrRestart(delivery, headers, attempt, consumerChannel, exception)
            }
        } else {
            publishDeadLetterOrRestart(delivery, headers, attempt, consumerChannel, exception)
        }
        logger.error("Product event {} moved past service processing", event.eventId, exception)
    }

    private fun publishRetryOrRestart(
        delivery: RabbitDelivery,
        headers: Map<String, Any>,
        nextAttempt: Int,
        consumerChannel: Channel
    ) {
        try {
            publisher.publishRetry(delivery.body, headers, nextAttempt)
            consumerChannel.basicAck(delivery.envelope.deliveryTag, false)
        } catch (publishFailure: Throwable) {
            throw ConsumerChannelRestartRequired(publishFailure)
        }
    }

    private fun publishDeadLetterOrRestart(
        delivery: RabbitDelivery,
        headers: Map<String, Any>,
        attempt: Int,
        consumerChannel: Channel,
        exception: Throwable
    ) {
        try {
            publisher.publishDeadLetter(
                body = delivery.body,
                headers = headers,
                errorType = exception::class.qualifiedName ?: "unknown",
                errorMessage = exception.message ?: "unknown error",
                attempt = attempt
            )
            consumerChannel.basicAck(delivery.envelope.deliveryTag, false)
        } catch (publishFailure: Throwable) {
            throw ConsumerChannelRestartRequired(publishFailure)
        }
    }

    private fun isTransient(exception: Throwable): Boolean =
        generateSequence(exception) { it.cause }.any {
            it is IOException ||
                    it is SQLTransientException ||
                    it is SQLRecoverableException ||
                    it is SQLException && it.sqlState?.startsWith("08") == true ||
                    it is TimeoutException
        }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ProductEventConsumer::class.java)
    }
}
