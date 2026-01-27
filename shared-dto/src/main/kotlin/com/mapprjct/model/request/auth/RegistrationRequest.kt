package com.mapprjct.model.request.auth

import com.mapprjct.model.dto.UserCredentials
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val phone: String,
    val username: String,
    val password: String,
)

fun com.mapprjct.model.request.auth.RegistrationRequest.toUserCredentialsDto() : com.mapprjct.model.dto.UserCredentials {
    return _root_ide_package_.com.mapprjct.model.dto.UserCredentials(
        phone = phone,
        password = password
    )
}
