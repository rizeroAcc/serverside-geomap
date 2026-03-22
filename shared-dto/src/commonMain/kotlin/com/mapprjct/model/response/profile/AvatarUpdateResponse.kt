package com.mapprjct.model.response.profile

import kotlinx.serialization.Serializable

@Serializable
data class AvatarUpdateResponse(
    val userDTO : com.mapprjct.model.dto.UserDTO,
)