package com.mapprjct.model.response.profile

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserInfoResponse(
    val  userDTO : com.mapprjct.model.dto.UserDTO
)
