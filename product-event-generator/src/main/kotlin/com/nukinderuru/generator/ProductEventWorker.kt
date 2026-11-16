package com.nukinderuru.generator

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.Duration

class ProductEventWorker(
    private val repository: ProductEventRepository,
    private val interval: Duration
) {
    private val logger = LoggerFactory.getLogger(ProductEventWorker::class.java)

    suspend fun run() {
        logger.info("Product event generator started with interval {}", interval)
        while (true) {
            try {
                val event = repository.generateRandomProductChange()
                if (event == null) {
                    logger.warn("No products found. Product change event was not generated")
                } else {
                    logger.info("Generated product change event: {}", event)
                }
            } catch (exception: Exception) {
                logger.error("Failed to generate product change event", exception)
            }
            delay(interval)
        }
    }
}