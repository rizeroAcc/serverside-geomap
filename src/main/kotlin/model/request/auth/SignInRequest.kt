package com.mapprjct.model.request.auth

import com.mapprjct.model.dto.UserCredentials
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val phone: String,
    val password: String
)

fun SignInRequest.toUserCredentialsDTO() : UserCredentials {
    return UserCredentials(
        phone = phone,
        password = password,
    )
}