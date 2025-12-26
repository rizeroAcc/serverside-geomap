package com.mapprjct.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordResponse(
    val expireAt: Long,
)
