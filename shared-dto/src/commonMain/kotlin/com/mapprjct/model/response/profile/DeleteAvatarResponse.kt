package com.mapprjct.model.response.profile

import kotlinx.serialization.Serializable

@Serializable
data class DeleteAvatarResponse(
    val userDTO : com.mapprjct.model.dto.UserDTO
)
