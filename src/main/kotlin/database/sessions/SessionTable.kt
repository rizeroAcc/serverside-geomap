package com.mapprjct.database.sessions

import org.jetbrains.exposed.sql.Table

object SessionTable : Table("sessions") {
    val id = varchar("id", 255)
    val data = text("data")
    val phone = varchar("phone", 12)

    override val primaryKey = PrimaryKey(id)
}