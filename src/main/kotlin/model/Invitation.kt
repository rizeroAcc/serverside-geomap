package com.mapprjct.model

import com.mapprjct.model.dto.Role
import java.util.UUID

data class Invitation(
    val inviterPhone : String,
    val inviteCode : UUID,
    val projectID : UUID,
    val expireAt : Long,
    val role : Role,
)
