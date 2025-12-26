package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.Table

object ProjectTable : Table("projects") {
    val id = uuid("id")
    val name = varchar("name", 80)
    val membersCount = short("members_count")
    override val primaryKey = PrimaryKey(id)
}