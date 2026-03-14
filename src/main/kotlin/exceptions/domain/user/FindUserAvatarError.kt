package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class FindUserAvatarError(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : FindUserAvatarError()
    class UserAvatarNotFound() : FindUserAvatarError()
    class Database(val exception : ExposedSQLException) : FindUserAvatarError()
    class Unexpected(override val cause: Throwable) : FindUserAvatarError(cause)
}