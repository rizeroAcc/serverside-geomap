package com.mapprjct.exceptions.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class FindUserException : Throwable() {
    class UserNotFound(val phone : String) : FindUserException()
    class DatabaseError(exception : ExposedSQLException) : FindUserException()
    class Unexpected(exception: Throwable) : FindUserException()
}