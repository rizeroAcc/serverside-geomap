package com.mapprjct.model.datatype

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    @SerialName("owner")
    Owner,
    @SerialName("admin")
    Admin,
    @SerialName("worker")
    Worker;

    fun toShort() : Short {
        return when (this) {
            Owner -> 1
            Admin -> 2
            Worker -> 3
        }
    }
    fun toInt() : Int {
        return toShort().toInt()
    }
}