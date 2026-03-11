package com.mapprjct.model.response.auth

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenResponse(
    val tokenExpireAt: Long,
)