package com.mapprjct.service

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.exceptions.invitation.InvitationDMLExceptions
import com.mapprjct.exceptions.invitation.InvitationValidationException
import com.mapprjct.exceptions.project.ProjectDMLException
import com.mapprjct.model.Invitation
import com.mapprjct.model.Role
import com.mapprjct.model.asRole
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

class InvitationService(
    val database: Database,
    val projectRepository: ProjectRepository,
    val invitationRepository: InvitationRepository
) {

    /**
     * @throws IllegalArgumentException - if project UUID or role code invalid
     * @throws IllegalStateException - if user already have 5 invitations
     * @throws InvitationValidationException.InvalidUserRole - if creating invitation for role Owner
     * @throws InvitationValidationException.UserNotStayInProject - if user not stay in project
     * @throws InvitationValidationException.NoPermissionToAddMembers - if user hasn't permission to add project members
     * @throws ProjectDMLException.ProjectNotFoundException - if project with ID doesn't exist
     * */
    @OptIn(ExperimentalTime::class)
    suspend fun createInvitation(
        inviterPhone : String,
        projectID : String,
        role : Short
    ) : Result<Invitation>{
        return runCatching {
            val projectUUID = UUID.fromString(projectID)
            val invitationRole = role.asRole()

            requireRoleIsNotOwner(invitationRole)
            requireProjectExists(projectID)

            suspendTransaction(database) {
                requireUserHavePermissionToInvite(inviterPhone,projectID)

                val newInvitation = Invitation(
                    inviterPhone = inviterPhone,
                    projectID = projectUUID,
                    expireAt = Clock.System.now().toEpochMilliseconds() + 24.hours.inWholeMilliseconds,
                    inviteCode = UUID.randomUUID(),
                    role = invitationRole
                )
                val invitation = invitationRepository.insertInvitation(newInvitation).getOrElse {
                    throw InvitationValidationException.TooManyInvitationsPerUser()
                }
                return@suspendTransaction invitation
            }
        }
    }
    //todo cover in tests
    /**
     * @throws IllegalArgumentException - if invite code isn't UUID
     * @throws InvitationDMLExceptions.InvitationNotFoundException - if invitation not found
     * */
    suspend fun getInvitation(inviteCode : String) : Result<Invitation> {
        return runCatching {
            val codeUUID = UUID.fromString(inviteCode)
            suspendTransaction(database) {
                invitationRepository.getInvitation(codeUUID) ?: throw InvitationDMLExceptions.InvitationNotFoundException(inviteCode)
            }
        }
    }
    suspend fun deleteInvitation(inviteCode : String) : Result<Unit> {
        return runCatching {
            suspendTransaction(database) {
                val codeUUID = UUID.fromString(inviteCode)
            }
        }
    }

    /**
     * @throws InvitationValidationException.InvalidUserRole - if role is Owner
     * */
    private fun requireRoleIsNotOwner(role: Role){
        if (role == Role.Owner)
            throw InvitationValidationException.InvalidUserRole(role)
    }

    /**
     * @throws ProjectDMLException.ProjectNotFoundException - if project doesn't exist
     * */
    private suspend fun requireProjectExists(projectID: String) = suspendTransaction(database) {
        projectRepository.getProjectById(UUID.fromString(projectID))
            ?: throw ProjectDMLException.ProjectNotFoundException(projectID)
    }

    /**
     *
     * --Must be called inside transaction--
     *
     * @throws InvitationValidationException.NoPermissionToAddMembers - if user hasn't permission to add members
     * @throws InvitationValidationException.UserNotStayInProject - if user doesn't stay in project
     * */
    private suspend fun requireUserHavePermissionToInvite(
        inviterPhone: String,
        projectID: String
    ) {
        val userMembership = projectRepository.getAllUserProjects(inviterPhone)
            .singleOrNull { it.project.projectID == projectID }
        if (userMembership != null) {
            //check user have permission to add members
            val currentRole = userMembership.role.toShort().asRole()
            if (currentRole == Role.Worker) {
                throw InvitationValidationException.NoPermissionToAddMembers(projectID)
            }
        } else {
            throw InvitationValidationException.UserNotStayInProject(projectID)
        }
    }
}