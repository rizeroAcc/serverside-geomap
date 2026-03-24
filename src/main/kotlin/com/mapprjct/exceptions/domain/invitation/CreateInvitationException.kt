package com.mapprjct.exceptions.domain.invitation

import com.mapprjct.model.datatype.Role
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface CreateInvitationException{
    data class InvalidInvitationRole(val role : Role) : CreateInvitationException
    data class InviterNotStayInProject(val projectID : String) : CreateInvitationException
    data class NoPermissionToAddMembers(val projectID : String) : CreateInvitationException
    data class ProjectNotFound(val projectID : String) : CreateInvitationException
    data class Database(val exception : ExposedSQLException) : CreateInvitationException
    data class Unexpected(val cause: Throwable) : CreateInvitationException
}