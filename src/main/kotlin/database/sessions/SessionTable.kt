package com.mapprjct.database.sessions

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object SessionTable : Table("session") {
    val id = varchar("id", 255)
    val data = text("data")
}