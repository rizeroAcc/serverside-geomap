package com.mapprjct.service

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.exceptions.invitation.InvitationDMLExceptions
import com.mapprjct.exceptions.project.ProjectDMLException
import com.mapprjct.exceptions.project.ProjectValidationException
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectWithRole
import java.util.UUID
import kotlin.Result.Companion.success

class ProjectService(
    val userRepository: UserRepository,
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
            userRepository.getUser(userPhone)
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
            userRepository.getUser(creatorPhone)
                ?: throw UserDMLExceptions.UserNotFoundException(creatorPhone)
            projectRepository.insertProject(creatorPhone, projectName)
        }
    }

    /**
     * @throws InvitationDMLExceptions.InvitationNotFoundException - if invitation not found
     * @throws ProjectValidationException.UserAlreadyProjectMember - if user already project member
     * @throws IllegalArgumentException - if invitation code invalid
     * */
    suspend fun joinProject(userPhone: String, invitationCode : String) : Result<Project>{
        return runCatching {
            val code = UUID.fromString(invitationCode)

            val invitation = invitationRepository.getInvitation(
                code = code
            ) ?: throw InvitationDMLExceptions.InvitationNotFoundException(invitationCode)

            if (userPhone == invitation.inviterPhone){
                throw ProjectValidationException.UserAlreadyProjectMember(
                    invitation.projectID.toString()
                )
            }

            val project = projectRepository.getProjectById(invitation.projectID)
                ?: throw ProjectDMLException.ProjectNotFoundException(invitation.projectID.toString())

            val userAlreadyStayInProject = projectRepository.getAllUserProjects(userPhone).map {
                it.project.projectID
            }.contains(invitation.projectID.toString())

            if (userAlreadyStayInProject){
                throw ProjectValidationException.UserAlreadyProjectMember(invitation.projectID.toString())
            }

            projectRepository.addMemberToProject(userPhone, project = project, role = invitation.role)

            invitationRepository.deleteInvitation(invitation.inviteCode)
            return success(project)
        }
    }
}