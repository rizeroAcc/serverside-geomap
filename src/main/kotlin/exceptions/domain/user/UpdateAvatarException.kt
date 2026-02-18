package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UpdateAvatarException(cause : Throwable? = null) : Throwable(cause) {
    class InvalidAvatarFormat(val allowedFormat : List<String>) : UpdateAvatarException()
    class FilesystemUnavailable : UpdateAvatarException()
    class ConnectionTerminated : UpdateAvatarException()
    class UserNotFound(val phone : String) : UpdateAvatarException()
    class DatabaseError(val exception : ExposedSQLException) : UpdateAvatarException()
    class Unexpected(override val cause: Throwable) : UpdateAvatarException(cause)
}