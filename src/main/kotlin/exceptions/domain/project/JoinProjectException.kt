package com.mapprjct.exceptions.domain.project

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class JoinProjectException(cause : Throwable? = null) : Throwable(cause) {
    class InvitationNotFound(val invitationCode : String) : JoinProjectException()
    class UserAlreadyProjectMember(val projectID : String) : JoinProjectException()
    class ProjectNotFound(val projectID : String) : JoinProjectException()
    class Database(val exception : ExposedSQLException) : JoinProjectException()
    class Unexpected(override val cause: Throwable) : JoinProjectException(cause)
}