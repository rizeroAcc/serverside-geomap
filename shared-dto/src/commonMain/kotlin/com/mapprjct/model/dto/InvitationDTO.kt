package com.mapprjct.model.dto

import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class InvitationDTO(
    val inviterPhone : RussiaPhoneNumber,
    val inviteCode : StringUUID,
    val projectID : StringUUID,
    val expireAt : Long,
    val role : Role,
)


