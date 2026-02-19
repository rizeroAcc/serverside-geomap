package com.mapprjct.model.request.auth

import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
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