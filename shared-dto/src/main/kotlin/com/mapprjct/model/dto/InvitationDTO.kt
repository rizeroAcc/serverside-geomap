package com.mapprjct.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvitationDTO(
    val inviterPhone : String,
    val inviteCode : String,
    val projectID : String,
    val expireAt : Long,
    val role : Short,
)


