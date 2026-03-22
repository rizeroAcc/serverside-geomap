package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface UpdateAvatarError{
    data class InvalidAvatarFormat(val allowedFormat : List<String>) : UpdateAvatarError
    data class UserNotFound(val phone : String) : UpdateAvatarError
    data object FilesystemUnavailable : UpdateAvatarError
    data object ConnectionTerminated : UpdateAvatarError

    data class Database(val exception : ExposedSQLException) : UpdateAvatarError
    data class Unexpected(val cause: Throwable) : UpdateAvatarError
}