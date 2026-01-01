package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.charLength
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater

object UserTable : Table("users"){
    val phone : Column<String> =
        varchar("phone", 11)
            .check(name = "phone_valid") {
                it.charLength() eq 11
            }
            .uniqueIndex()
    val username : Column<String> =
        varchar("username", 80)
            .check(name = "username_valid") {
                it.charLength() greater 0
            }
    val passwordHash : Column<String> = varchar("passwordHash", 255)
    val avatar : Column<String?> = text("avatar").nullable()

    override val primaryKey = PrimaryKey(phone)

}