package com.nukinderuru.data.db.dao

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

object SupplierTable : UUIDTable("suppliers") {
    val name = varchar("name", 128)
    val address = reference("address_id", AddressTable)
    val phoneNumber = varchar("phone_number", 32)
}

class SupplierEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SupplierEntity>(SupplierTable)

    var name by SupplierTable.name
    var address by AddressEntity referencedOn SupplierTable.address
    var phoneNumber by SupplierTable.phoneNumber
}
