package com.mapprjct.service

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
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
    @OptIn(ExperimentalTime::class)
    suspend fun createInvitation(
        inviterPhone : String,
        projectID : String,
        role : Short
    ) : Result<Invitation>{
        var projectUUID : UUID? = null
        try {
            projectUUID = UUID.fromString(projectID)
        }catch (e : IllegalArgumentException){
            return Result.failure(IllegalArgumentException("Invalid projectID"))
        }

        val project = projectRepository.getProjectById(projectUUID)
            ?: return Result.failure(IllegalStateException("Project with ID $projectID not found"))

        var invitationRole : Role? = null

        try {
            invitationRole = role.asRole()
            if (invitationRole == Role.Owner)
                throw IllegalArgumentException("Can't create invitation with owner role")
        }catch (e : IllegalArgumentException){
            return Result.failure(e)
        }

        val newInvitation = Invitation(
            inviterPhone = inviterPhone,
            projectID = projectUUID!!,
            expireAt = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 24,
            inviteCode = UUID.randomUUID(),
            role = role.asRole()
        )
        val invitation = invitationRepository.insertInvitationCode(newInvitation)
        return invitation
    }
}