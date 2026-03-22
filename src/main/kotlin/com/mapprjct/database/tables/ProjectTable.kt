package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.charLength
import org.jetbrains.exposed.v1.core.greater

object ProjectTable : Table("projects") {
    val id = uuid("id").uniqueIndex()
    val name = varchar("name", 80).check { it.charLength() greater 0 }
    val membersCount = short("members_count")
    override val primaryKey = PrimaryKey(id)

}