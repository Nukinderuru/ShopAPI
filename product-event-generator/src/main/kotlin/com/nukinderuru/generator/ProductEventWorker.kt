package com.nukinderuru.generator

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

class ProductEventWorker(
    private val repository: ProductEventRepository,
    private val publisher: ProductEventPublisher,
    private val interval: Duration
) {
    private val logger = LoggerFactory.getLogger(ProductEventWorker::class.java)

    suspend fun run() = coroutineScope {
        logger.info("Product event generator started with interval {}", interval)

        while (isActive) {
            try {
                generateAndPublishEvent()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.error("Failed to generate or publish product change event", exception)
            }
            delay(interval)
        }
    }

    internal suspend fun generateAndPublishEvent() {
        val event = repository.generateRandomProductChange()
        if (event == null) {
            logger.warn("No products found. Product change event was not generated")
            return
        }
        logger.info("Generated product change event: {}", event)

        publisher.publish(event)
        logger.info("Published product change event: {}", event)
    }
}