package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.InvitationRepository
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.dto.asRole
import com.mapprjct.model.Invitation
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class InvitationRepositoryImpl(val database: Database) : InvitationRepository{
    override suspend fun insertInvitationCode(
        invitation : Invitation
    ): Invitation? {
        val createdInvitation = transaction(database) {
            val inviteCodeCount = InviteCodeTable.selectAll().where{
                InviteCodeTable.inviterPhone eq invitation.inviterPhone
                InviteCodeTable.projectID eq invitation.projectID
            }.count()
            if (inviteCodeCount < 5){
                InviteCodeTable.insert {
                    it[InviteCodeTable.inviterPhone] = invitation.inviterPhone
                    it[InviteCodeTable.projectID] = invitation.projectID
                    it[InviteCodeTable.expireAt] = invitation.expireAt
                    it[InviteCodeTable.inviteCode] = invitation.inviteCode
                    it[InviteCodeTable.role] = invitation.role.toShort()
                }
                return@transaction invitation
            }else{
                return@transaction null
            }
        }
        return createdInvitation
    }

    override suspend fun getInvitation(code: UUID): Invitation? {
        return transaction(database) {
            InviteCodeTable.selectAll().where{
                InviteCodeTable.inviteCode eq code
            }.singleOrNull()?.let {
                Invitation(
                    inviterPhone = it[InviteCodeTable.inviterPhone],
                    inviteCode = it[InviteCodeTable.inviteCode],
                    projectID = it[InviteCodeTable.projectID],
                    expireAt = it[InviteCodeTable.expireAt],
                    role = it[InviteCodeTable.role].asRole()
                )
            }
        }
    }

    override suspend fun deleteInvitationCode(invitation: Invitation) {
        InviteCodeTable.deleteWhere{
            InviteCodeTable.inviteCode eq invitation.inviteCode
        }
    }
}