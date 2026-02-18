package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class DeleteUserAvatarException(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : DeleteUserAvatarException()
    class UserAvatarNotFound() : DeleteUserAvatarException()
    class FileSystemUnavailable() : DeleteUserAvatarException()
    class DatabaseError(val exception : ExposedSQLException) : DeleteUserAvatarException()
    class Unexpected(override val cause: Throwable) : DeleteUserAvatarException(cause)
}