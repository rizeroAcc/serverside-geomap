package com.mapprjct.database.repository

import com.mapprjct.model.Invitation
import java.util.UUID

interface InvitationRepository {
    /**
     * @throws IllegalStateException - if user already have 5 invitations
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException - with code 23503 if user with phone doesn't exist
     * */
    suspend fun insertInvitation(invitation: Invitation) : Result<Invitation>
    suspend fun getInvitation(code: UUID) : Invitation?
    suspend fun deleteInvitation(inviteCode : UUID) : Int
}