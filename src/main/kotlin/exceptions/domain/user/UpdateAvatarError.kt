package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UpdateAvatarError(cause : Throwable? = null) : Throwable(cause) {
    class InvalidAvatarFormat(val allowedFormat : List<String>) : UpdateAvatarError()
    class FilesystemUnavailable : UpdateAvatarError()
    class ConnectionTerminated : UpdateAvatarError()
    class UserNotFound(val phone : String) : UpdateAvatarError()
    class Database(val exception : ExposedSQLException) : UpdateAvatarError()
    class Unexpected(override val cause: Throwable) : UpdateAvatarError(cause)
}