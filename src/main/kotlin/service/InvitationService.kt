package com.mapprjct.service

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.exceptions.invitation.InvitationValidationException
import com.mapprjct.exceptions.project.ProjectDMLException
import com.mapprjct.model.Invitation
import com.mapprjct.model.dto.Role
import com.mapprjct.model.dto.asRole
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class InvitationService(
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {

    /**
     * @throws IllegalArgumentException - if project UUID or role code invalid
     * @throws IllegalStateException - if user already have 5 invitations
     * @throws InvitationValidationException.InvalidUserRole - if creating invitation for role Owner
     * @throws ProjectDMLException.ProjectNotFoundException - if project with ID doesn't exists
     * */
    @OptIn(ExperimentalTime::class)
    suspend fun createInvitation(
        inviterPhone : String,
        projectID : String,
        role : Short
    ) : Result<Invitation>{
        return runCatching {
            val projectUUID = UUID.fromString(projectID)

            projectRepository.getProjectById(projectUUID)
                ?: throw ProjectDMLException.ProjectNotFoundException(projectID)

            val invitationRole = role.asRole()
            if (invitationRole == Role.Owner)
                throw InvitationValidationException.InvalidUserRole(invitationRole)

            val newInvitation = Invitation(
                inviterPhone = inviterPhone,
                projectID = projectUUID,
                expireAt = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 24,
                inviteCode = UUID.randomUUID(),
                role = invitationRole
            )
            val invitation = invitationRepository.insertInvitationCode(newInvitation)
            return invitation
        }

    }
}