package com.mapprjct.database.dao

import com.mapprjct.model.Invitation
import java.util.UUID

interface InvitationDAO {
    suspend fun insertInvitationCode(invitation: Invitation) : Invitation?
    suspend fun getInvitaion(code: UUID) : Invitation?
}