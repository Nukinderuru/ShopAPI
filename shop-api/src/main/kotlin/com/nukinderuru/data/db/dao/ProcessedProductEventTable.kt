package com.nukinderuru.data.db.dao

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ProcessedProductEventTable : Table("processed_product_events") {
    val eventId = uuid("event_id")
    val productId = uuid("product_id")
    val processedAt = datetime("processed_at")

    override val primaryKey = PrimaryKey(eventId)
}