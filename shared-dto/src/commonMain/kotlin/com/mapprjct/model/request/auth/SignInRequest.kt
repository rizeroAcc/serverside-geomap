package com.mapprjct.model.request.auth

import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.value.Password
import com.mapprjct.model.value.RussiaPhoneNumber
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val phone: RussiaPhoneNumber,
    val password: Password
)

fun SignInRequest.toUserCredentialsDTO() : UserCredentials {
    return UserCredentials(
        phone = phone,
        password = password,
    )
}