package com.mapprjct.feature.authentication.authorization

import com.mapprjct.database.users.UserDTO
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(val phone: String, val passwordHash: String)

fun SignInRequest.toUserDTO() : UserDTO {
    return UserDTO(
        phone = phone,
        passwordHash = passwordHash,
        username = ""
    )
}