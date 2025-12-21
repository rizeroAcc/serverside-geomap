package com.mapprjct.request

import kotlinx.serialization.Serializable

@Serializable
data class ChangeUserInfoRequest(
    val username: String? = null,
)
