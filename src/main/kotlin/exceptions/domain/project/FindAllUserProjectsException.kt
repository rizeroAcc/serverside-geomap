package com.mapprjct.exceptions.domain.project

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class FindAllUserProjectsException(cause: Throwable? = null) : Throwable(cause) {
    class UserNotFound(val phone : String) : FindAllUserProjectsException()
    class Database(val exception : ExposedSQLException) : FindAllUserProjectsException()
    class Unexpected(override val cause: Throwable) : FindAllUserProjectsException(cause)
}