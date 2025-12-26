package com.mapprjct.model.response

import kotlinx.serialization.Serializable

@Serializable
data class AvatarUpdateResponse(
    val avatarUrl: String,
)