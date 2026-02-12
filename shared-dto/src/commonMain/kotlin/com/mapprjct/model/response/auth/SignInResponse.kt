package com.mapprjct.model.response.auth

import com.mapprjct.model.dto.User
import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    val user : User,
    val tokenExpiration : Long,
) {
}