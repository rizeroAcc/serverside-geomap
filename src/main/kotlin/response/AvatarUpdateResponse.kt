package com.mapprjct.response

import kotlinx.serialization.Serializable

@Serializable
data class AvatarUpdateResponse(
    val avatarUrl: String,
)