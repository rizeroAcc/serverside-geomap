package com.mapprjct.exceptions.domain.user

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class ValidateCredentialError(cause : Throwable? = null) : Throwable(cause) {
    class Database(val exception : ExposedSQLException) : ValidateCredentialError()
    class Unexpected(override val cause: Throwable) : ValidateCredentialError(cause)
}