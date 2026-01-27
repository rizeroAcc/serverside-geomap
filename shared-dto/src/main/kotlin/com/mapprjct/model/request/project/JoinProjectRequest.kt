package com.mapprjct.model.request.project

import kotlinx.serialization.Serializable

@Serializable
data class JoinProjectRequest(
    val inviteCode: String,
)