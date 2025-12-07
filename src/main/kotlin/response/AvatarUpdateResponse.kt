package com.mapprjct.response

import kotlinx.serialization.Serializable

@Serializable
data class AvatarUpdateResponse(
    val avatarUrl: String,
    val success: Boolean,
    val message : String,
)