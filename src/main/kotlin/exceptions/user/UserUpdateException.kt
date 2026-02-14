package com.mapprjct.exceptions.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UserUpdateException : Throwable() {
    class UserNotFound(val phone : String) : UserUpdateException()
    class DatabaseError(val exception : ExposedSQLException) : UserUpdateException()
    class Unexpected(val exception: Throwable) : UserUpdateException()
}