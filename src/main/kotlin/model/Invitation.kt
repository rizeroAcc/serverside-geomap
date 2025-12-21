package com.mapprjct.model

import com.mapprjct.dto.Role
import kotlinx.serialization.Serializable
import java.util.UUID

data class Invitation(
    val inviterPhone : String,
    val inviteCode : UUID,
    val projectID : UUID,
    val expireAt : Long,
    val role : Role,
)
