package com.mapprjct.response

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordResponse(
    val expireAt: Long,
)
