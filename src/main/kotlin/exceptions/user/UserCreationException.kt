package com.mapprjct.exceptions.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class UserCreationException {
    class UserAlreadyExists(val phone : String) : UserCreationException()
    class DatabaseError(exception : ExposedSQLException) : UserCreationException()
    class Unexpected(exception: Throwable) : UserCreationException()
}