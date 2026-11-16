package com.nukinderuru.generator

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object ProductTable: Table("products") {
    val id = uuid("id")
    val price = decimal("price", 12, 2)
    val availableStock = integer("available_stock")
    val lastUpdateDate = date("last_update_date")

    override val primaryKey = PrimaryKey(id)
}