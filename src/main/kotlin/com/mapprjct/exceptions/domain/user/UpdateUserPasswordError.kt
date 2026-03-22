package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UpdateUserPasswordError(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : UpdateUserPasswordError()
    class IncorrectPassword : UpdateUserPasswordError()
    class Database(val exception : ExposedSQLException) : UpdateUserPasswordError()
    class Unexpected(override val cause: Throwable) : UpdateUserPasswordError(cause)
}