package com.mapprjct.service

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectWithRole
import com.mapprjct.model.dto.Role
import com.mapprjct.model.dto.asRole
import com.mapprjct.model.Invitation
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ProjectService(
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {
    suspend fun getAllUserProjects(userPhone : String): Result<List<ProjectWithRole>>{
        return try {
            Result.success(projectRepository.getAllUserProjects(userPhone))
        }catch (e : Exception){
            Result.failure(e)
        }
    }
    suspend fun createProject(creatorPhone : String, projectName : String) : Result<Project>{
        return try {
            Result.success(projectRepository.insertProject(creatorPhone, projectName))
        }catch (e : Exception){
            Result.failure(e)
        }
    }
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
    //todo make check to
    suspend fun joinProject(userPhone: String, invitationCode : String) : Result<Project>{
        val invitation = invitationRepository.getInvitation(
            code = UUID.fromString(invitationCode)
        )
        if (invitation == null){
            return Result.failure(IllegalArgumentException(
                "Invitation ${invitationCode} not found, may be it expired"
            ))
        }
        if (userPhone == invitation.inviterPhone){
            return Result.failure(IllegalStateException("User already project member"))
        }
        val project = projectRepository.getProjectById(invitation.projectID)
        if (project == null){
            return Result.failure(IllegalStateException(
                "Project with ID ${invitation.projectID} not found, may be it was deleted")
            )
        }
        try {
            projectRepository.addMemberToProject(userPhone, project = project, role = invitation.role)
        }catch (e: Exception){
            return Result.failure(e)
        }

        return Result.success(project)
    }
}