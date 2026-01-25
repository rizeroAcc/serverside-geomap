package com.mapprjct.model.response.project

import com.mapprjct.model.Invitation
import kotlinx.serialization.Serializable

@Serializable
data class CreateInvitationResponse(
    val inviterPhone : String,
    val inviteCode : String,
    val projectID : String,
    val expireAt : Long,
    val role : Short,
){
    companion object{
        fun fromInvitation(invitation: Invitation):CreateInvitationResponse{
            return CreateInvitationResponse(
                inviterPhone = invitation.inviterPhone,
                inviteCode = invitation.inviteCode.toString(),
                projectID = invitation.projectID.toString(),
                expireAt = invitation.expireAt,
                role = invitation.role.toShort()
            )
        }
    }
}