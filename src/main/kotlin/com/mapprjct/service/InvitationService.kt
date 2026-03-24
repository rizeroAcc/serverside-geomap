package com.mapprjct.service

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.mapprjct.com.mapprjct.utils.TransactionProvider
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.exceptions.domain.invitation.CreateInvitationException
import com.mapprjct.exceptions.domain.invitation.FindInvitationException
import com.mapprjct.model.Invitation
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

class InvitationService(
    val transactionProvider: TransactionProvider,
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {
    //todo Нет ошибки на случай, когда пользователь пытается создать приглашение с ролью выше своей (может и не надо)
    @OptIn(ExperimentalTime::class)
    suspend fun createInvitation(
        inviterPhone : RussiaPhoneNumber,
        projectID : StringUUID,
        role : Role
    ) : Either<CreateInvitationException, Invitation> = either {
        catch({
            ensure(role != Role.Owner) {
                CreateInvitationException.InvalidInvitationRole(role)
            }

            transactionProvider.runInTransaction {
                ensureNotNull(projectRepository.getProjectById(projectID.toUUID())){
                    CreateInvitationException.ProjectNotFound(projectID.value)
                }
                val userMembership = projectRepository.findUserMembershipInProject(inviterPhone,projectID.toUUID())
                ensureNotNull(userMembership){
                    CreateInvitationException.InviterNotStayInProject(projectID.value)
                }
                ensure(userMembership.role != Role.Worker){
                    CreateInvitationException.NoPermissionToAddMembers(projectID.value)
                }

                val newInvitation = Invitation(
                    inviterPhone = inviterPhone,
                    projectID = projectID.toUUID(),
                    expireAt = Clock.System.now().toEpochMilliseconds() + 24.hours.inWholeMilliseconds,
                    inviteCode = UUID.randomUUID(),
                    role = role
                )
                invitationRepository.insertInvitation(newInvitation)
            }
        }){ error->
            when(error){
                is ExposedSQLException -> raise(CreateInvitationException.Database(error))
                else -> raise(CreateInvitationException.Unexpected(error))
            }
        }
    }

    //todo cover in tests
    suspend fun getInvitation(inviteCode : StringUUID) : Either<FindInvitationException, Invitation> = either {
        catch({
            transactionProvider.runInTransaction {
                invitationRepository.getInvitation(inviteCode.toUUID()) ?: raise(FindInvitationException.NotFound(inviteCode.value))
            }
        }){ error->
            when(error){
                is ExposedSQLException-> raise(FindInvitationException.Database(error))
                else -> raise(FindInvitationException.Unexpected(error))
            }
        }
    }

    suspend fun deleteInvitation(inviteCode : String) : Either<Unit, Any> = either {
        TODO()
    }
}