package com.mapprjct.database.daoimpl

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.model.dto.asRole
import com.mapprjct.model.Invitation
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.insertReturning
import java.util.UUID

class InvitationRepositoryImpl(val database: Database) : InvitationRepository{
    override suspend fun insertInvitationCode(
        invitation : Invitation
    ): Result<Invitation> {
        val inviteCodeCount = InviteCodeTable.selectAll().where{
            InviteCodeTable.inviterPhone eq invitation.inviterPhone
            InviteCodeTable.projectID eq invitation.projectID
        }.count()
        return if (inviteCodeCount < 5){
            InviteCodeTable.insert {
                it[InviteCodeTable.inviterPhone] = invitation.inviterPhone
                it[InviteCodeTable.projectID] = invitation.projectID
                it[InviteCodeTable.expireAt] = invitation.expireAt
                it[InviteCodeTable.inviteCode] = invitation.inviteCode
                it[InviteCodeTable.role] = invitation.role.toShort()
            }
            Result.success(invitation)
        }else{
            Result.failure(IllegalArgumentException("Attempt to register over five invitations"))
        }
    }

    override suspend fun getInvitation(code: UUID): Invitation? {
        return InviteCodeTable
            .selectAll()
            .where{ InviteCodeTable.inviteCode eq code }
            .singleOrNull()?.let {
                Invitation(
                    inviterPhone = it[InviteCodeTable.inviterPhone],
                    inviteCode = it[InviteCodeTable.inviteCode],
                    projectID = it[InviteCodeTable.projectID],
                    expireAt = it[InviteCodeTable.expireAt],
                    role = it[InviteCodeTable.role].asRole()
                )
            }
    }

    override suspend fun deleteInvitationCode(invitation: Invitation) : Int {
        return InviteCodeTable.deleteWhere {
                InviteCodeTable.inviteCode eq invitation.inviteCode
            }
    }
}