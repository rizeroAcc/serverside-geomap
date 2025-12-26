package com.mapprjct.database.entity

import com.mapprjct.database.tables.UserTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

class UserEntity(id : EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserEntity>(UserTable)

    val phone by UserTable.phone
    var password by UserTable.passwordHash
    var username by UserTable.username
    var avatar by UserTable.avatar
}