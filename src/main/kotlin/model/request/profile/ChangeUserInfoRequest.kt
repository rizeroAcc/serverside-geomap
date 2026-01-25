package com.mapprjct.model.request.profile

import com.mapprjct.model.dto.User
import kotlinx.serialization.Serializable

@Serializable
data class ChangeUserInfoRequest(
    val user : User,
)