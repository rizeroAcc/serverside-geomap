package com.mapprjct.model.request.project

import kotlinx.serialization.Serializable

@Serializable
data class CreateInvitationRequest(
    val projectID : String,
    val role : Short,
)