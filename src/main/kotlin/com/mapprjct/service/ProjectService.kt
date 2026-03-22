package com.mapprjct.service


import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.mapprjct.com.mapprjct.utils.TransactionProvider
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.exceptions.domain.project.ProjectRegistrationError
import com.mapprjct.exceptions.domain.project.FindAllUserProjectsException
import com.mapprjct.exceptions.domain.project.FindProjectException
import com.mapprjct.exceptions.domain.project.JoinProjectException
import com.mapprjct.model.dto.ProjectDTO
import com.mapprjct.model.dto.ProjectMembershipDTO
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.ProjectRegistrationResultDTO
import com.mapprjct.model.dto.UnregisteredProjectDTO
import com.mapprjct.utils.toUUID
import io.ktor.client.engine.callContext
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ProjectService(
    val transactionProvider: TransactionProvider,
    val userRepository: UserRepository,
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {
    suspend fun getProject(projectID : StringUUID) : Either<FindProjectException, ProjectDTO> = either {
        catch( {
            transactionProvider.runInTransaction {
                projectRepository.getProjectById(projectID.toUUID())
                    ?: raise(FindProjectException.NotFound(projectID.value))
            }
        }){ error->
            when(error){
                is ExposedSQLException -> raise(FindProjectException.Database(error))
                else -> raise(FindProjectException.Unexpected(error))
            }
        }
    }
    suspend fun getAllUserProjects(userPhone : RussiaPhoneNumber): Either<FindAllUserProjectsException, List<ProjectMembershipDTO>> = either {
        catch( {
            transactionProvider.runInTransaction {
                projectRepository.findAllUserProjects(userPhone)
            }
        }){ error->
            when(error){
                is ExposedSQLException-> raise(FindAllUserProjectsException.Database(error))
                else -> raise(FindAllUserProjectsException.Unexpected(error))
            }
        }
    }
    suspend fun registerProject(creatorPhone : RussiaPhoneNumber, unregisteredProjectDTO : UnregisteredProjectDTO) : Either<ProjectRegistrationError, ProjectRegistrationResultDTO> = either {
        ensure(unregisteredProjectDTO.name.isNotBlank()){
            ProjectRegistrationError.BlankProjectName
        }
        catch({
            transactionProvider.runInTransaction {
                ensureNotNull(userRepository.findUser(creatorPhone)){
                    ProjectRegistrationError.UserNotFound(creatorPhone.value)
                }
                projectRepository.insert(creatorPhone, unregisteredProjectDTO)
            }
        }){ error ->
            when(error){
                is ExposedSQLException -> raise(ProjectRegistrationError.Database(error))
                else -> raise(ProjectRegistrationError.Unexpected(error))
            }
        }
    }
    suspend fun registerProjectList(creatorPhone: RussiaPhoneNumber, unregisteredProjectDTOS : List<UnregisteredProjectDTO>) : Either<ProjectRegistrationError, List<ProjectRegistrationResultDTO>> = either {
        unregisteredProjectDTOS.forEach { project->
            ensure(project.name.isNotBlank()){
                ProjectRegistrationError.BlankProjectName
            }
        }
        catch({
            transactionProvider.runInTransaction {
                ensureNotNull(userRepository.findUser(creatorPhone)) {
                    ProjectRegistrationError.UserNotFound(creatorPhone.value)
                }
                projectRepository.insertAll(creatorPhone, unregisteredProjectDTOS)
            }
        }){ error->
            when(error){
                is ExposedSQLException -> raise(ProjectRegistrationError.Database(error))
                else -> raise(ProjectRegistrationError.Unexpected(error))
            }
        }
    }

    //TODO Уже по логике UseCase. Потом можно вынести
    suspend fun joinProject(userPhone: RussiaPhoneNumber, invitationCode : StringUUID) : Either<JoinProjectException, ProjectDTO> = either {
        catch({
            val nowTime = Clock.System.now().toEpochMilliseconds()
            transactionProvider.runInTransaction {
                val invitation = ensureNotNull(invitationRepository.getInvitation(invitationCode.toUUID())) {
                    JoinProjectException.InvitationNotFound(invitationCode.value)
                }

                ensure(invitation.expireAt > nowTime){
                    JoinProjectException.InvitationExpired(invitationCode.value)
                }

                ensure (userPhone != invitation.inviterPhone){
                    JoinProjectException.UserAlreadyProjectMember(invitation.projectID.toString())
                }

                val project = projectRepository.getProjectById(invitation.projectID)
                ensureNotNull(project) {
                    JoinProjectException.ProjectNotFound(invitation.projectID.toString())
                }

                ensure(projectRepository.findUserMembershipInProject(userPhone,invitation.projectID) == null){
                    JoinProjectException.UserAlreadyProjectMember(invitation.projectID.toString())
                }

                val updatedProjectMembership = projectRepository.addMemberToProject(userPhone, projectDTO = project, role = invitation.role)
                invitationRepository.deleteInvitation(invitation.inviteCode)
                updatedProjectMembership.projectDTO
            }
        }) { error->
            when(error){
                is ExposedSQLException-> raise(JoinProjectException.Database(error))
                else -> raise(JoinProjectException.Unexpected(error))
            }
        }
    }
}