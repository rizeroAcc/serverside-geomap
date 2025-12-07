package com.mapprjct.database.tables

import org.jetbrains.exposed.sql.Table

object ProjectTable : Table("projects") {
    val id = uuid("id")
    val name = varchar("name", 80)

    override val primaryKey = PrimaryKey(id)
}