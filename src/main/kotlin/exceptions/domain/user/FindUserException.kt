package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class FindUserException(cause : Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : FindUserException()
    class Database(val exception : ExposedSQLException) : FindUserException()
    class Unexpected(override val cause: Throwable) : FindUserException(cause)
}