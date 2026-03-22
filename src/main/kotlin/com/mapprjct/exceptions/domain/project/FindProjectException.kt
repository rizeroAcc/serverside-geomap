package com.mapprjct.exceptions.domain.project

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface FindProjectException {
    data class NotFound(val projectID : String) : FindProjectException
    data class Database(val exception : ExposedSQLException) : FindProjectException
    data class Unexpected(val cause: Throwable) : FindProjectException
}