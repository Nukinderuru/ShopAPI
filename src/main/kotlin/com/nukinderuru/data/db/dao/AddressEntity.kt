package com.nukinderuru.data.db.dao

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

object AddressTable : UUIDTable("addresses") {
    val country = varchar("country", 128)
    val city = varchar("city", 128)
    val street = varchar("street", 256)
}

class AddressEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AddressEntity>(AddressTable)

    var country by AddressTable.country
    var city by AddressTable.city
    var street by AddressTable.street
}
