package com.mapprjct.utils

import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.StringUUID
import java.util.UUID

/**
 * @throws IllegalArgumentException - if role code incorrect
 * */
fun Short.asRole(): Role {
    return when (this) {
        1.toShort() -> Role.Owner
        2.toShort() -> Role.Admin
        3.toShort()-> Role.Worker
        else -> {
            throw IllegalArgumentException("Unknown role")
        }
    }
}

fun StringUUID.toUUID(): UUID = UUID.fromString(this.value)
fun UUID.toStringUUID() : StringUUID = StringUUID(this.toString())