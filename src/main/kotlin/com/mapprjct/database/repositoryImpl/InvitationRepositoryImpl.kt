package com.mapprjct.database.daoimpl

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.model.Invitation
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.utils.asRole
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertReturning
import java.util.UUID

class InvitationRepositoryImpl() : InvitationRepository{
    override suspend fun insertInvitation(
        invitation : Invitation
    ): Invitation {
        return InviteCodeTable.insertReturning(
            returning = InviteCodeTable.columns
        ) {
            it[InviteCodeTable.inviterPhone] = invitation.inviterPhone.normalizeAsRussiaPhone()
            it[InviteCodeTable.projectID] = invitation.projectID
            it[InviteCodeTable.expireAt] = invitation.expireAt
            it[InviteCodeTable.inviteCode] = invitation.inviteCode
            it[InviteCodeTable.role] = invitation.role.toShort()
        }.single().toInvitation()
    }

    override suspend fun getInvitation(inviteCode: UUID): Invitation? {
        return InviteCodeTable
            .selectAll()
            .where{ InviteCodeTable.inviteCode eq inviteCode }
            .singleOrNull()?.toInvitation()
    }

    override suspend fun deleteInvitation(inviteCode: UUID) : Int {
        return InviteCodeTable.deleteWhere {
            InviteCodeTable.inviteCode eq inviteCode
        }
    }

    fun ResultRow.toInvitation() : Invitation {
        return Invitation(
            inviterPhone = RussiaPhoneNumber(this[InviteCodeTable.inviterPhone]),
            inviteCode = this[InviteCodeTable.inviteCode],
            projectID = this[InviteCodeTable.projectID],
            expireAt = this[InviteCodeTable.expireAt],
            role = this[InviteCodeTable.role].asRole()
        )
    }
}