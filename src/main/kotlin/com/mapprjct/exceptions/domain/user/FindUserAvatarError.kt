package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface FindUserAvatarError{
    data class UserNotFound(val phone : String) : FindUserAvatarError
    data object UserAvatarNotFound : FindUserAvatarError
    data class Database(val exception : ExposedSQLException) : FindUserAvatarError
    data class Unexpected(val cause: Throwable) : FindUserAvatarError
}