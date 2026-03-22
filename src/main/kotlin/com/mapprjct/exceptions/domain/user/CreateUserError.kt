package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class CreateUserError(cause: Throwable? = null) : Throwable(cause) {
    class UserAlreadyExists(val phone : String) : CreateUserError()
    class Database(val exception : ExposedSQLException) : CreateUserError()
    class Unexpected(override val cause: Throwable) : CreateUserError(cause)
}