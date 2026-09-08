package com.nukinderuru.data.db.dao

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

object ProductTable : UUIDTable("products") {
    val name = varchar("name", 128)
    val category = varchar("category", 128)
    val price = decimal("price", 12, 2)
    val availableStock = integer("available_stock")
    val lastUpdateDate = date("last_update_date")
    val supplier = optReference("supplier_id", SupplierTable)
    val image = optReference("image_id", ImageTable)
}

class ProductEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProductEntity>(ProductTable)

    var name by ProductTable.name
    var category by ProductTable.category
    var price: BigDecimal by ProductTable.price
    var availableStock by ProductTable.availableStock
    var lastUpdateDate: LocalDate by ProductTable.lastUpdateDate
    var supplier by SupplierEntity optionalReferencedOn ProductTable.supplier
    var image by ImageEntity optionalReferencedOn ProductTable.image
}
