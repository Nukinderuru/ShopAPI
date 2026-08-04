package com.nukinderuru.data.db.dao

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

object ImageTable : UUIDTable("images") {
    val image = binary("image")
}

class ImageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ImageEntity>(ImageTable)

    var image by ImageTable.image
}
