package com.mapprjct.model.request.profile

import kotlinx.serialization.Serializable

@Serializable
data class ChangeUserInfoRequest(
    val userDTO : com.mapprjct.model.dto.UserDTO,
)