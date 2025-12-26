package com.mapprjct.model.request

import kotlinx.serialization.Serializable

@Serializable
data class ChangeUserInfoRequest(
    val username: String? = null,
)
