package com.mapprjct.database.users

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users"){
    val phone = varchar("phone", 12)
    val username = varchar("username", 80)
    val passwordHash = varchar("passwordHash", 255)
}