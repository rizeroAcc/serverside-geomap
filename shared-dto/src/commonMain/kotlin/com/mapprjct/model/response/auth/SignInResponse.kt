package com.mapprjct.model.response.auth

import com.mapprjct.model.dto.UserDTO
import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    val userDTO : UserDTO,
    val tokenExpiration : Long,
) {
}