package com.mapprjct.feature.authentication.registration

import com.mapprjct.database.users.UserDTO
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val phone: String,
    val username: String,
    val password: String,
)

fun RegistrationRequest.toUserDto() : UserDTO{
    return UserDTO(
        phone = phone,
        username = username,
        passwordHash = password
    )
}
