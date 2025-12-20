package com.mapprjct.response

import com.mapprjct.dto.User
import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    val user : User,
    val tokenExpiration : Long,
) {
}