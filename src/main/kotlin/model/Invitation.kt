package com.mapprjct.model

import com.mapprjct.model.datatype.Role
import com.mapprjct.model.dto.InvitationDTO
import com.mapprjct.model.response.project.CreateInvitationResponse
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.utils.toStringUUID
import com.mapprjct.utils.toUUID
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
        inviterPhone = RussiaPhoneNumber(inviterPhone.value),
        inviteCode = inviteCode.toStringUUID(),
        projectID = projectID.toStringUUID(),
        expireAt = expireAt,
        role = role
    )
}
/**
 * @throws IllegalArgumentException - if role, phone, or UUID convertation failed
 * */
fun InvitationDTO.toInvitation() : Invitation {
    return Invitation(
        inviterPhone = this.inviterPhone,
        inviteCode = this.inviteCode.toUUID(),
        projectID = this.projectID.toUUID(),
        expireAt = this.expireAt,
        role = this.role
    )
}

fun createInvitationResponseFromInvitation(invitation: Invitation) : CreateInvitationResponse{
    return CreateInvitationResponse(
        invitationDTO = invitation.toDTO()
    )
}
