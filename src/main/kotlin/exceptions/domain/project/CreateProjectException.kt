package com.mapprjct.exceptions.domain.project

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class CreateProjectException(cause : Throwable? = null) : Throwable() {
    class UserNotFound(val phone : String) : CreateProjectException()
    class InvalidProjectName(override val message : String) : CreateProjectException()
    class Database(val exception : ExposedSQLException) : CreateProjectException()
    class Unexpected(override val cause: Throwable) : CreateProjectException(cause)
}