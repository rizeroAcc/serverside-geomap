package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UserUpdateException(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : UserUpdateException()
    class DatabaseError(val exception : ExposedSQLException) : UserUpdateException()
    class Unexpected(override val cause: Throwable) : UserUpdateException(cause)
}