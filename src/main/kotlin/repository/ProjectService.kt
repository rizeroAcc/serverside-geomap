package com.mapprjct.repository

import com.mapprjct.NotFoundException
import com.mapprjct.database.dao.InvitationRepository
import com.mapprjct.database.dao.ProjectRepository
import com.mapprjct.dto.Project
import com.mapprjct.dto.ProjectWithRole
import com.mapprjct.dto.Role
import com.mapprjct.dto.asRole
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
            ?: return Result.failure(NotFoundException("Project with ID $projectID not found"))

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
        return if (invitation == null){
            Result.failure(IllegalStateException("User have over five invitations in this project"))
        }else{
            Result.success(invitation)
        }
    }
    //todo make check to
    suspend fun joinProject(userPhone: String, invitationCode : String) : Result<Project>{
        val invitation = invitationRepository.getInvitaion(
            code = UUID.fromString(invitationCode)
        )
        if (invitation == null){
            return Result.failure(NotFoundException(
                "Invitation ${invitationCode} not found, may be it expired"
            ))
        }
        if (userPhone == invitation.inviterPhone){
            return Result.failure(IllegalStateException("User already project member"))
        }
        val project = projectRepository.getProjectById(invitation.projectID)
        if (project == null){
            return Result.failure(NotFoundException(
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