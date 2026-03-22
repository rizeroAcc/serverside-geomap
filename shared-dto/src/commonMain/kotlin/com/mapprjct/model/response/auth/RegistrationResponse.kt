package com.mapprjct.model.response.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponse(
    val userDTO : com.mapprjct.model.dto.UserDTO
)