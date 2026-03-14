package com.mapprjct.model.datatype

import com.mapprjct.model.datatype.Role.Admin
import com.mapprjct.model.datatype.Role.Owner
import com.mapprjct.model.datatype.Role.Worker
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

fun Int.asRole() : Role{
    return when(this){
        1 -> Owner
        2 -> Admin
        3 -> Worker
        else -> throw IllegalArgumentException()
    }
}