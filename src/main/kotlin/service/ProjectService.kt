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
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID
import kotlin.Result.Companion.success

class ProjectService(
    val database: Database,
    val userRepository: UserRepository,
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {
    /**
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException - if database unavailable
     * @throws IllegalArgumentException - if project id is not valid UUID
     * @throws ProjectDMLException.ProjectNotFoundException - if project not found
     * */
    suspend fun getProject(projectID : String) : Result<Project>{
        return runCatching {
            val projectUUID = UUID.fromString(projectID)
            suspendTransaction {
                projectRepository.getProjectById(projectUUID)
                    ?: throw ProjectDMLException.ProjectNotFoundException(projectID)
            }
        }

    }
    /**
     * [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     *
     * [UserDMLExceptions.UserNotFoundException] - if user doesn't exists
     * */
    suspend fun getAllUserProjects(userPhone : String): Result<List<ProjectWithRole>>{
        return runCatching {
            suspendTransaction(database) {
                userRepository.getUser(userPhone)
                    ?: throw UserDMLExceptions.UserNotFoundException(userPhone)
                projectRepository.getAllUserProjects(userPhone)
            }
        }
    }
    /**
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException - if database unavailable
     * @throws ProjectValidationException.EmptyProjectName - if project name empty
     * @throws UserDMLExceptions.UserNotFoundException - if user doesn't exists
     * */
    suspend fun createProject(creatorPhone : String, projectName : String) : Result<Project>{
        return runCatching {
            if(projectName.isBlank()){
                throw ProjectValidationException.EmptyProjectName()
            }
            suspendTransaction(database) {
                userRepository.getUser(creatorPhone)
                    ?: throw UserDMLExceptions.UserNotFoundException(creatorPhone)
                projectRepository.insertProject(creatorPhone, projectName)
            }
        }.recover { exception ->
            return when(exception){
                is ExposedSQLException ->{
                    if (exception.sqlState == "23514"){
                       Result.failure(ProjectValidationException.EmptyProjectName())
                    }else{
                        Result.failure(exception)
                    }
                }else -> {
                    Result.failure(exception)
                }
            }
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
            return suspendTransaction(database) {
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
                success(project.copy(membersCount = project.membersCount + 1))
            }

        }
    }
}