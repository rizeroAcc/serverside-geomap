package com.mapprjct.exceptions.domain.project

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface ProjectRegistrationError {
    data class UserNotFound(val phone : String) : ProjectRegistrationError
    data object BlankProjectName : ProjectRegistrationError
    data class Database(val exception : ExposedSQLException) : ProjectRegistrationError
    data class Unexpected(val cause: Throwable) : ProjectRegistrationError
}