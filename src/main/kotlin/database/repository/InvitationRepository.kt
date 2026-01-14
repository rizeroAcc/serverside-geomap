package com.mapprjct.database.repository

import com.mapprjct.model.Invitation
import java.util.UUID

interface InvitationRepository {
    /**
     * @throws IllegalStateException if user already have 5 invitations
     * */
    suspend fun insertInvitation(invitation: Invitation) : Result<Invitation>
    suspend fun getInvitation(code: UUID) : Invitation?
    suspend fun deleteInvitation(inviteCode : UUID) : Int
}