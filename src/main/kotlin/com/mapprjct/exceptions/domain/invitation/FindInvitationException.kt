package com.mapprjct.exceptions.domain.invitation

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface FindInvitationException {
    data class NotFound(val inviteCode : String) : FindInvitationException
    data class Database(val exception : ExposedSQLException) : FindInvitationException
    data class Unexpected( val cause: Throwable) : FindInvitationException
}