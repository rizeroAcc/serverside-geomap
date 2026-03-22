package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface DeleteUserAvatarError {
    data object UserNotFound : DeleteUserAvatarError
    data object UserAvatarNotFound : DeleteUserAvatarError
    data object FileSystemUnavailable : DeleteUserAvatarError
    data class Database(val exception : ExposedSQLException) : DeleteUserAvatarError
    data class Unexpected(val cause: Throwable) : DeleteUserAvatarError
}