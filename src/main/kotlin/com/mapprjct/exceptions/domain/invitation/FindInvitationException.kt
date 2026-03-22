package com.mapprjct.exceptions.domain.invitation

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class FindInvitationException(cause : Throwable? = null) : Throwable() {
    class NotFound(val inviteCode : String) : FindInvitationException()
    class Database(val exception : ExposedSQLException) : FindInvitationException()
    class Unexpected(override val cause: Throwable) : FindInvitationException(cause)
}