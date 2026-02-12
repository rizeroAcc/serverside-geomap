package com.mapprjct.model.response.profile

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordResponse(
    val expireAt: Long,
)