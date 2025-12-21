package com.mapprjct.request

import kotlinx.serialization.Serializable

@Serializable
data class InviteUserRequest(
    val projectID : String,
    val role : Short,
)
