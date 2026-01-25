package com.mapprjct.model.response.profile

import com.mapprjct.model.dto.User
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserInfoResponse(
    val  user : User
)
