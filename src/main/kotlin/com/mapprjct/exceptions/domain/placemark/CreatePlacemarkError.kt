package com.mapprjct.exceptions.domain.placemark

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface CreatePlacemarkError{
    data object EmptyName : CreatePlacemarkError
    data class UserNotStayInProject(val projectID: String) : CreatePlacemarkError
    data class NoPermissionToCreatePlacemark(val projectID: String) : CreatePlacemarkError
    data class ProjectNotFound(val projectID : String) : CreatePlacemarkError
    data class Database(val exception : ExposedSQLException) : CreatePlacemarkError
    data class Unexpected(val cause: Throwable) : CreatePlacemarkError
}