package com.mapprjct.exceptions.domain.invitation

import com.mapprjct.exceptions.domain.project.JoinProjectException
import com.mapprjct.model.Role
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class CreateInvitationException(cause : Throwable? = null) : Throwable(cause) {
    class InvalidInvitationRole(val role : Role) : CreateInvitationException()
    class InviterNotStayInProject(val projectID : String) : CreateInvitationException()
    class NoPermissionToAddMembers(val projectID : String) : CreateInvitationException()
    class ProjectNotFound(val projectID : String) : CreateInvitationException()
    class Database(val exception : ExposedSQLException) : CreateInvitationException()
    class Unexpected(override val cause: Throwable) : CreateInvitationException(cause)
}