package com.mapprjct.request

import com.mapprjct.dto.UserCredentials
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val phone: String,
    val password: String
)

fun SignInRequest.toUserCredentialsDTO() : UserCredentials {
    return UserCredentials(
        phone = phone,
        passwordHash = password,
        username = ""
    )
}