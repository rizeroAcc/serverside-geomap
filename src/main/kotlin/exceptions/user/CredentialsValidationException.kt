package com.mapprjct.exceptions.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class CredentialsValidationException() {
    class DatabaseError(exception : ExposedSQLException) : CredentialsValidationException()
    class Unexpected(exception: Throwable) : CredentialsValidationException()
}