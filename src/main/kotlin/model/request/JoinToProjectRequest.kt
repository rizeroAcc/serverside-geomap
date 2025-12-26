package com.mapprjct.model.request

import kotlinx.serialization.Serializable

@Serializable
data class JoinToProjectRequest(
    val inviteCode: String,
)
