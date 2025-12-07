package com.mapprjct.response

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    val phone: String,
    val username: String,
    //In future can be added new fields
) {
}