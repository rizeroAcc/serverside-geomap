package com.mapprjct.request

import kotlinx.serialization.Serializable

@Serializable
data class JoinToProjectRequest(
    val inviteCode: String,
)
