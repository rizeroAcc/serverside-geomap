package com.mapprjct.model.request.auth

import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.Username
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val phone: RussiaPhoneNumber,
    val username: Username,
    val password: Password,
)

fun RegistrationRequest.toUserCredentialsDto() : UserCredentials {
    return UserCredentials(
        phone = phone,
        password = password
    )
}
