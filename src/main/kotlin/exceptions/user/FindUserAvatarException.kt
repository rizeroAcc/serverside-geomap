package com.mapprjct.exceptions.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class FindUserAvatarException : Throwable() {
    class UserNotFound(val phone : String) : FindUserAvatarException()
    class UserAvatarNotFound() : FindUserAvatarException()
    class DatabaseError(val exception : ExposedSQLException) : FindUserAvatarException()
    class Unexpected(val exception: Throwable) : FindUserAvatarException()
}