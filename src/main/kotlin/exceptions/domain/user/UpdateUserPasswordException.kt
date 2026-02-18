package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UpdateUserPasswordException(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : UpdateUserPasswordException()
    class IncorrectPassword : UpdateUserPasswordException()
    class DatabaseError(val exception : ExposedSQLException) : UpdateUserPasswordException()
    class Unexpected(override val cause: Throwable) : UpdateUserPasswordException(cause)
}