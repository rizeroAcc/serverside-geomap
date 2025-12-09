package com.mapprjct.request

import com.mapprjct.dto.UserCredentials
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val phone: String,
    val username: String,
    val password: String,
)

fun RegistrationRequest.toUserCredentialsDto() : UserCredentials{
    return UserCredentials(
        phone = phone,
        password = password
    )
}
