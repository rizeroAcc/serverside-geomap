package com.mapprjct.service

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.exceptions.domain.project.CreateProjectException
import com.mapprjct.exceptions.domain.project.FindAllUserProjectsException
import com.mapprjct.exceptions.domain.project.FindProjectException
import com.mapprjct.exceptions.domain.project.JoinProjectException
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectMembership
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.UnregisteredProject
import com.mapprjct.utils.Either
import com.mapprjct.utils.toEither
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class ProjectService(
    val database: Database,
    val userRepository: UserRepository,
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {
    suspend fun getProject(projectID : StringUUID) : Either<Project, FindProjectException>{
        return runCatching {
            suspendTransaction {
                projectRepository.getProjectById(projectID.toUUID())
                    ?: throw FindProjectException.NotFound(projectID.value)
            }
        }.toEither { error->
            when(error){
                is ExposedSQLException -> FindProjectException.Database(error)
                else -> FindProjectException.Unexpected(error)
            }
        }
    }
    suspend fun getAllUserProjects(userPhone : RussiaPhoneNumber): Either<List<ProjectMembership>, FindAllUserProjectsException>{
        return runCatching {
            suspendTransaction(database) {
                userRepository.findUser(userPhone)
                    ?: throw FindAllUserProjectsException.UserNotFound(userPhone.value)
                projectRepository.findAllUserProjects(userPhone)
            }
        }.toEither { error->
            when(error){
                is ExposedSQLException-> FindAllUserProjectsException.Database(error)
                else -> throw FindAllUserProjectsException.Unexpected(error)
            }
        }
    }
    suspend fun registerProject(creatorPhone : RussiaPhoneNumber, unregisteredProject : UnregisteredProject) : Either<Pair<Project, StringUUID?>, CreateProjectException>{
        return runCatching {
            if(unregisteredProject.name.isBlank()){
                throw CreateProjectException.InvalidProjectName("Project Name can't be blank")
            }
            suspendTransaction(database) {
                userRepository.findUser(creatorPhone)
                    ?: throw CreateProjectException.UserNotFound(creatorPhone.value)
                projectRepository.insert(creatorPhone, unregisteredProject)
            }
        }.toEither { error ->
            when(error){
                is ExposedSQLException -> CreateProjectException.Database(error)
                else -> CreateProjectException.Unexpected(error)
            }
        }
    }
    suspend fun registerProjectList(creatorPhone: RussiaPhoneNumber, unregisteredProjects : List<UnregisteredProject>) : Either<List<Pair<Project, StringUUID?>>, CreateProjectException> {
        return runCatching {
            unregisteredProjects.forEach { project->
                if (project.name.isBlank()){
                    throw CreateProjectException.InvalidProjectName("Project Name can't be blank")
                }
            }
            suspendTransaction(database) {
                userRepository.findUser(creatorPhone)
                    ?: throw CreateProjectException.UserNotFound(creatorPhone.value)
                projectRepository.insertAll(creatorPhone, unregisteredProjects)
            }
        }.toEither { error->
            when(error){
                is ExposedSQLException -> CreateProjectException.Database(error)
                else -> CreateProjectException.Unexpected(error)
            }
        }
    }

    //TODO Уже по логике UseCase. Потом можно вынести
    suspend fun joinProject(userPhone: RussiaPhoneNumber, invitationCode : StringUUID) : Either<Project, JoinProjectException>{
        return runCatching {
            suspendTransaction(database) {
                val invitation = invitationRepository.getInvitation(invitationCode.toUUID())
                    ?: throw JoinProjectException.InvitationNotFound(invitationCode.value)

                if (userPhone == invitation.inviterPhone){
                    throw JoinProjectException.UserAlreadyProjectMember(invitation.projectID.toString())
                }

                val project = projectRepository.getProjectById(invitation.projectID)
                    ?: throw JoinProjectException.ProjectNotFound(invitation.projectID.toString())

                requireUserNotStayInProject(userPhone,invitation.projectID.toString())

                projectRepository.addMemberToProject(userPhone, project = project, role = invitation.role)
                invitationRepository.deleteInvitation(invitation.inviteCode)
                project.copy(membersCount = project.membersCount + 1)
            }
        }.toEither { error->
            when(error){
                is ExposedSQLException-> JoinProjectException.Database(error)
                else -> JoinProjectException.Unexpected(error)
            }
        }
    }

    /**
     * --Must be called inside transaction--
     * @throws JoinProjectException.UserAlreadyProjectMember - if user already stay in project
     * */
    private suspend fun requireUserNotStayInProject(userPhone: RussiaPhoneNumber, projectID : String){
        val userAlreadyStayInProject = projectRepository.findAllUserProjects(userPhone).map {
            it.project.projectID.value
        }.contains(projectID)

        if (userAlreadyStayInProject){
            throw JoinProjectException.UserAlreadyProjectMember(projectID)
        }
    }
}