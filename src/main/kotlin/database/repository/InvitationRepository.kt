package com.mapprjct.database.repository

import com.mapprjct.model.Invitation
import java.util.UUID

interface InvitationRepository {
    suspend fun insertInvitationCode(invitation: Invitation) : Result<Invitation>
    suspend fun getInvitation(code: UUID) : Invitation?
    suspend fun deleteInvitationCode(invitation: Invitation) : Int
}