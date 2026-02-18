package com.mapprjct.database.daoimpl

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.model.asRole
import com.mapprjct.model.Invitation
import com.mapprjct.model.value.RussiaPhoneNumber
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertReturning
import java.util.UUID

class InvitationRepositoryImpl(val database: Database) : InvitationRepository{
    override suspend fun insertInvitation(
        invitation : Invitation
    ): Int {
        return InviteCodeTable.insert {
            it[InviteCodeTable.inviterPhone] = invitation.inviterPhone.value
            it[InviteCodeTable.projectID] = invitation.projectID
            it[InviteCodeTable.expireAt] = invitation.expireAt
            it[InviteCodeTable.inviteCode] = invitation.inviteCode
            it[InviteCodeTable.role] = invitation.role.toShort()
        }.insertedCount
    }

    override suspend fun getInvitation(code: UUID): Invitation? {
        return InviteCodeTable
            .selectAll()
            .where{ InviteCodeTable.inviteCode eq code }
            .singleOrNull()?.let {
                Invitation(
                    inviterPhone = RussiaPhoneNumber(it[InviteCodeTable.inviterPhone]),
                    inviteCode = it[InviteCodeTable.inviteCode],
                    projectID = it[InviteCodeTable.projectID],
                    expireAt = it[InviteCodeTable.expireAt],
                    role = it[InviteCodeTable.role].asRole()
                )
            }
    }

    override suspend fun deleteInvitation(inviteCode: UUID) : Int {
        return InviteCodeTable.deleteWhere {
                InviteCodeTable.inviteCode eq inviteCode
            }
    }
}