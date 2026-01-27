package com.mapprjct.model.response.auth

import com.mapprjct.model.dto.User
import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    val user : com.mapprjct.model.dto.User,
    val tokenExpiration : Long,
) {
}