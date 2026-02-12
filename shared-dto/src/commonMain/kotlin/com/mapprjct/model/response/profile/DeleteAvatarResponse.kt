package com.mapprjct.model.response.profile

import com.mapprjct.model.dto.User
import kotlinx.serialization.Serializable

@Serializable
data class DeleteAvatarResponse(
    val user : com.mapprjct.model.dto.User
)
