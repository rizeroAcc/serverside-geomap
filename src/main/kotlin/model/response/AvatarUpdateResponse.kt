package com.mapprjct.model.response

import com.mapprjct.model.dto.User
import kotlinx.serialization.Serializable

@Serializable
data class AvatarUpdateResponse(
    val user : User,
)