package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UserCreationException(cause: Throwable? = null) : Throwable(cause) {
    class UserAlreadyExists(val phone : String) : UserCreationException()
    class Database(val exception : ExposedSQLException) : UserCreationException()
    class Unexpected(override val cause: Throwable) : UserCreationException(cause)
}