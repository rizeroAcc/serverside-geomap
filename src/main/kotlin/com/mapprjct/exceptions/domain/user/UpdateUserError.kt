package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UpdateUserError(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : UpdateUserError()
    class Database(val exception : ExposedSQLException) : UpdateUserError()
    class Unexpected(override val cause: Throwable) : UpdateUserError(cause)
}