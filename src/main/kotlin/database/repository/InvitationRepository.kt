package com.mapprjct.database.dao

import com.mapprjct.model.Invitation
import java.util.UUID

interface InvitationRepository {
    suspend fun insertInvitationCode(invitation: Invitation) : Invitation?
    suspend fun getInvitation(code: UUID) : Invitation?
    suspend fun deleteInvitationCode(invitation: Invitation)
}