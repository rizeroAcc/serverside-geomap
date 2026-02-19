package com.mapprjct.database.repository

import com.mapprjct.model.Invitation
import java.util.UUID

interface InvitationRepository {
    suspend fun insertInvitation(invitation: Invitation) : Int
    suspend fun getInvitation(inviteCode: UUID) : Invitation?
    suspend fun deleteInvitation(inviteCode : UUID) : Int
}