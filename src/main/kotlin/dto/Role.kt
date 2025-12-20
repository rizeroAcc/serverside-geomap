package com.mapprjct.dto

enum class Role {
    Owner,
    Admin,
    Worker;

    fun toInt() : Int {
        return when (this) {
            Role.Owner -> 1
            Role.Admin -> 2
            Role.Worker -> 3
        }
    }
}

fun Short.asRole(): Role {
    return when (this) {
        1.toShort() -> Role.Owner
        2.toShort() -> Role.Admin
        3.toShort()-> Role.Worker
        else -> {
            throw IllegalStateException("Unknown role")
        }
    }
}