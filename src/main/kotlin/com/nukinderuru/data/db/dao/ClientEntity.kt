package com.nukinderuru.data.db.dao

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate
import java.util.UUID

@Serializable
enum class Gender {
    MALE,
    FEMALE,
    OTHER
}

object ClientTable : UUIDTable("clients") {
    val clientName = varchar("client_name", 128)
    val clientSurname = varchar("client_surname", 128)
    val birthday = date("birthday")
    val gender = enumerationByName("gender", 32, Gender::class)
    val registrationDate = date("registration_date")
    val address = reference("address_id", AddressTable)
}

class ClientEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ClientEntity>(ClientTable)

    var clientName by ClientTable.clientName
    var clientSurname by ClientTable.clientSurname
    var birthday: LocalDate by ClientTable.birthday
    var gender by ClientTable.gender
    var registrationDate: LocalDate by ClientTable.registrationDate
    var address by AddressEntity.Companion referencedOn ClientTable.address
}
