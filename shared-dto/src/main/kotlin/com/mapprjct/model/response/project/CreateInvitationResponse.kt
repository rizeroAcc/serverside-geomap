package com.mapprjct.model.response.project

import com.mapprjct.model.dto.InvitationDTO
import kotlinx.serialization.Serializable

@Serializable
data class CreateInvitationResponse(
    val invitationDTO: InvitationDTO
)
