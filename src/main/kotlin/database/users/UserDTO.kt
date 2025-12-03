package com.mapprjct.database.users

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val phone : String,
    val username : String,
    val passwordHash : String,
)
