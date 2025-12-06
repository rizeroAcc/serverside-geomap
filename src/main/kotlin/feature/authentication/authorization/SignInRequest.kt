package com.mapprjct.feature.authentication.authorization

import com.mapprjct.database.users.UserDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val phone: String,
    @SerialName("password")
    val passwordHash: String
)

fun SignInRequest.toUserDTO() : UserDTO {
    return UserDTO(
        phone = phone,
        passwordHash = passwordHash,
        username = ""
    )
}