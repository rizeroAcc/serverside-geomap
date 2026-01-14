package com.mapprjct.model.request

import com.mapprjct.model.dto.User
import kotlinx.serialization.Serializable

@Serializable
data class ChangeUserInfoRequest(
    val user : User,
)
