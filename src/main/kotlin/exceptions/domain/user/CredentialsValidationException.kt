package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class CredentialsValidationException(cause : Throwable? = null) : Throwable(cause) {
    class Database(val exception : ExposedSQLException) : CredentialsValidationException()
    class Unexpected(override val cause: Throwable) : CredentialsValidationException(cause)
}