package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class FindUserAvatarException(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : FindUserAvatarException()
    class UserAvatarNotFound() : FindUserAvatarException()
    class DatabaseError(val exception : ExposedSQLException) : FindUserAvatarException()
    class Unexpected(override val cause: Throwable) : FindUserAvatarException(cause)
}