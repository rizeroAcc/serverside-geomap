package com.mapprjct.exceptions.domain.project

import com.mapprjct.exceptions.domain.user.CredentialsValidationException
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class FindProjectException(cause : Throwable? = null) : Throwable(cause) {
    class NotFound(val projectID : String) : FindProjectException()
    class Database(val exception : ExposedSQLException) : FindProjectException()
    class Unexpected(override val cause: Throwable) : FindProjectException(cause)
}