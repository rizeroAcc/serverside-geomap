package com.mapprjct.service

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.exceptions.invitation.InvitationDMLExceptions
import com.mapprjct.exceptions.project.ProjectDMLException
import com.mapprjct.exceptions.project.ProjectValidationException
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectWithRole
import com.mapprjct.model.dto.Role
import com.mapprjct.model.dto.asRole
import com.mapprjct.model.Invitation
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ProjectService(
    val userService: UserService,
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {

    /**
     * [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     *
     * [UserDMLExceptions.UserNotFoundException] - if user doesn't exists
     * */
    suspend fun getAllUserProjects(userPhone : String): Result<List<ProjectWithRole>>{
        return runCatching {
            userService.getUser(userPhone).getOrThrow()
                ?: throw UserDMLExceptions.UserNotFoundException(userPhone)
            projectRepository.getAllUserProjects(userPhone)
        }
    }
    /**
     * [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     *
     * [UserDMLExceptions.UserNotFoundException] - if user doesn't exists
     * */
    suspend fun createProject(creatorPhone : String, projectName : String) : Result<Project>{
        return runCatching {
            userService.getUser(creatorPhone).getOrThrow()
                ?: throw UserDMLExceptions.UserNotFoundException(creatorPhone)
            projectRepository.insertProject(creatorPhone, projectName)
        }
    }

    suspend fun joinProject(userPhone: String, invitationCode : String) : Result<Project>{
        return runCatching {
            val invitation = invitationRepository.getInvitation(
                code = UUID.fromString(invitationCode)
            ) ?: throw InvitationDMLExceptions.InvitationNotFoundException(invitationCode)

            if (userPhone == invitation.inviterPhone){
                throw ProjectValidationException.UserAlreadyProjectMember(
                    invitation.projectID.toString()
                )
            }

            val project = projectRepository.getProjectById(invitation.projectID)
                ?: throw ProjectDMLException.ProjectNotFoundException(invitation.projectID.toString())

            projectRepository.addMemberToProject(userPhone, project = project, role = invitation.role)

            return Result.success(project)
        }
    }
}