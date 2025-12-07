package com.mapprjct.database.tables

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users"){
    val phone = varchar("phone", 12)
    val username = varchar("username", 80)
    val passwordHash = varchar("passwordHash", 255)
    val avatar = text("avatar").nullable()



    override val primaryKey = PrimaryKey(phone)
}