package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class DeleteUserAvatarError(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : DeleteUserAvatarError()
    class UserAvatarNotFound() : DeleteUserAvatarError()
    class FileSystemUnavailable() : DeleteUserAvatarError()
    class Database(val exception : ExposedSQLException) : DeleteUserAvatarError()
    class Unexpected(override val cause: Throwable) : DeleteUserAvatarError(cause)
}