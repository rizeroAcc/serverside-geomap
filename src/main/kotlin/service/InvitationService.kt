package com.mapprjct.service

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.exceptions.domain.invitation.CreateInvitationException
import com.mapprjct.exceptions.domain.invitation.FindInvitationException
import com.mapprjct.model.Invitation
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.utils.Either
import com.mapprjct.utils.toEither
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

class InvitationService(
    val database: Database,
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {
    @OptIn(ExperimentalTime::class)
    suspend fun createInvitation(
        inviterPhone : RussiaPhoneNumber,
        projectID : StringUUID,
        role : Role
    ) : Either<Invitation, CreateInvitationException>{
        return runCatching {
            val projectUUID = projectID.toUUID()

            if (role == Role.Owner) throw CreateInvitationException.InvalidInvitationRole(role)

            suspendTransaction(database) {

                projectRepository.getProjectById(projectUUID)
                    ?: throw CreateInvitationException.ProjectNotFound(projectID.value)

                requireUserHavePermissionToInvite(inviterPhone,projectID.value)

                val newInvitation = Invitation(
                    inviterPhone = inviterPhone,
                    projectID = projectUUID,
                    expireAt = Clock.System.now().toEpochMilliseconds() + 24.hours.inWholeMilliseconds,
                    inviteCode = UUID.randomUUID(),
                    role = role
                )
                invitationRepository.insertInvitation(newInvitation)
                return@suspendTransaction newInvitation
            }
        }.toEither { error->
            when(error){
                is ExposedSQLException -> CreateInvitationException.Database(error)
                else -> CreateInvitationException.Unexpected(error)
            }
        }
    }

    //todo cover in tests
    suspend fun getInvitation(inviteCode : StringUUID) : Either<Invitation, FindInvitationException> {
        return runCatching {
            suspendTransaction(database) {
                invitationRepository.getInvitation(inviteCode.toUUID()) ?: throw FindInvitationException.NotFound(inviteCode.value)
            }
        }.toEither { error->
            when(error){
                is ExposedSQLException-> FindInvitationException.Database(error)
                else -> FindInvitationException.Unexpected(error)
            }
        }
    }

    suspend fun deleteInvitation(inviteCode : String) : Result<Unit> {
        TODO()
    }

    /**
     * --Must be called inside transaction--
     *
     * @throws CreateInvitationException.NoPermissionToAddMembers - if user hasn't permission to add members
     * @throws CreateInvitationException.InviterNotStayInProject - if user doesn't stay in project
     * */
    private suspend fun requireUserHavePermissionToInvite(
        inviterPhone: RussiaPhoneNumber,
        projectID: String
    ) {
        val userMembership = projectRepository
            .findUserMembershipInProject(inviterPhone, UUID.fromString(projectID))
        if (userMembership != null) {
            //check user have permission to add members
            val currentRole = userMembership.role
            if (currentRole == Role.Worker) {
                throw CreateInvitationException.NoPermissionToAddMembers(projectID)
            }
        } else {
            throw CreateInvitationException.InviterNotStayInProject(projectID)
        }
    }
}