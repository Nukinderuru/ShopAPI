package com.nukinderuru.generator

import com.nukinderuru.events.ProductChangeEvent
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.random.Random

class ProductEventRepository(
    private val database: Database,
    private val random: Random = Random.Default
) {
    suspend fun generateRandomProductChange(): ProductChangeEvent? = dbQuery {
        val product = findRandomProduct() ?: return@dbQuery null
        val oldPrice = product[ProductTable.price]
        val oldAvailableStock = product[ProductTable.availableStock]
        val newPrice = generateNewPrice(oldPrice)
        val newAvailableStock = generateNewAvailableStock(oldAvailableStock)

        ProductChangeEvent(
            productId = product[ProductTable.id],
            oldPrice = oldPrice,
            newPrice = newPrice,
            oldAvailableStock = oldAvailableStock,
            newAvailableStock = newAvailableStock
        )
    }

    private fun findRandomProduct(): ResultRow? {
        val totalProducts = ProductTable.selectAll().count()
        if (totalProducts == 0L) {
            return null
        }

        return ProductTable.selectAll()
            .orderBy(ProductTable.id to SortOrder.ASC)
            .limit(1, offset = random.nextLong(totalProducts))
            .single()
    }

    private fun generateNewPrice(currentPrice: BigDecimal): BigDecimal {
        val percentDelta = random.nextNonZeroInt(MIN_PRICE_DELTA_PERCENT, MAX_PRICE_DELTA_PERCENT)
        val multiplier = BigDecimal.valueOf(100L + percentDelta).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        val generatedPrice = currentPrice.multiply(multiplier)
            .setScale(2, RoundingMode.HALF_UP)
            .max(MIN_PRICE)
        return if (generatedPrice.compareTo(currentPrice) == 0) {
            currentPrice.add(MIN_PRICE_STEP)
        } else {
            generatedPrice
        }
    }

    private fun generateNewAvailableStock(currentStock: Int): Int {
        val delta = random.nextNonZeroInt(MIN_STOCK_DELTA, MAX_STOCK_DELTA)
        return max(0, currentStock + delta)
    }

    private fun Random.nextNonZeroInt(min: Int, max: Int): Int {
        require(min < 0 && max > 0) { "Range min must be negative and max must be positive" }
        var value: Int
        do {
            value = nextInt(min, max + 1)
        } while (value == 0)

        return value
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    private companion object {
        val MIN_PRICE = BigDecimal("0.01")
        val MIN_PRICE_STEP = BigDecimal("0.01")
        const val MIN_PRICE_DELTA_PERCENT = -20
        const val MAX_PRICE_DELTA_PERCENT = 20
        const val MIN_STOCK_DELTA = -10
        const val MAX_STOCK_DELTA = 10
    }
}