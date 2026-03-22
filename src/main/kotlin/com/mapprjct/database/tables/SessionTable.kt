package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.Table

object SessionTable : Table("sessions") {
    val id = varchar("id", 255).uniqueIndex()
    val data = text("data")
    val phone = varchar("phone", 12)

    override val primaryKey = PrimaryKey(id)
}