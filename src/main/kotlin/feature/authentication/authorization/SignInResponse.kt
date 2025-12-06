package com.mapprjct.feature.authentication.authorization

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    val phone: String,
    val username: String,
    //In future can be added new fields
) {
}