package com.mapprjct.model

import com.mapprjct.model.dto.InvitationDTO
import com.mapprjct.model.response.project.CreateInvitationResponse
import com.mapprjct.model.value.RussiaPhoneNumber
import java.util.UUID

data class Invitation(
    val inviterPhone : RussiaPhoneNumber,
    val inviteCode : UUID,
    val projectID : UUID,
    val expireAt : Long,
    val role : Role,
)

fun Invitation.toDTO() : InvitationDTO {
    return InvitationDTO(
        inviterPhone = inviterPhone.value,
        inviteCode = inviteCode.toString(),
        projectID = projectID.toString(),
        expireAt = expireAt,
        role = role.toShort()
    )
}
/**
 * @throws IllegalArgumentException - if role, phone, or UUID convertation failed
 * */
fun InvitationDTO.toInvitation() : Invitation {
    return Invitation(
        inviterPhone = RussiaPhoneNumber(this.inviterPhone),
        inviteCode = UUID.fromString(this.inviteCode),
        projectID = UUID.fromString(this.projectID),
        expireAt = this.expireAt,
        role = this.role.asRole()
    )
}

fun createInvitationResponseFromInvitation(invitation: Invitation) : CreateInvitationResponse{
    return CreateInvitationResponse(
        invitationDTO = invitation.toDTO()
    )
}
