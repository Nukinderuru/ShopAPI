package com.school21.auth.data.dao

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object UserTable : UUIDTable("auth_users") {
    val email = varchar("email", 256).uniqueIndex()
    val firstName = varchar("first_name", 128)
    val lastName = varchar("last_name", 128)
    val phone = varchar("phone", 32)
    val passwordHash = varchar("password_hash", 512)
    val passwordSalt = varchar("password_salt", 128)
    val createdAt = timestamp("created_at")
}

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UserTable)

    var email by UserTable.email
    var firstName by UserTable.firstName
    var lastName by UserTable.lastName
    var phone by UserTable.phone
    var passwordHash by UserTable.passwordHash
    var passwordSalt by UserTable.passwordSalt
    var createdAt by UserTable.createdAt
}
