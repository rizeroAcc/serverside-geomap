package com.mapprjct.model.dto

enum class Role {
    Owner,
    Admin,
    Worker;

    fun toShort() : Short {
        return when (this) {
            Role.Owner -> 1
            Role.Admin -> 2
            Role.Worker -> 3
        }
    }
}

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