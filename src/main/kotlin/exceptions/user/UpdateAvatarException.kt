package com.mapprjct.exceptions.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UpdateAvatarException : Throwable() {
    class InvalidAvatarFormat : UpdateAvatarException()
    class FilesystemUnavailable : UpdateAvatarException()
    class ConnectionTerminated : UpdateAvatarException()
    class UserNotFound(val phone : String) : UpdateAvatarException()
    class DatabaseError(val exception : ExposedSQLException) : UpdateAvatarException()
    class Unexpected(val exception: Throwable) : UpdateAvatarException()
}