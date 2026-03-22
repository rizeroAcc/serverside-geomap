package com.mapprjct.exceptions.domain.placemark

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed class CreatePlacemarkError(cause : Throwable? = null) : Throwable(cause)  {
    class EmptyProjectName : CreatePlacemarkError()
    class UserNotStayInProject(val projectID: String) : CreatePlacemarkError()
    class NoPermissionToCreatePlacemark(val projectID: String) : CreatePlacemarkError()
    class ProjectNotFound(val projectID : String) : CreatePlacemarkError()
    class Database(val exception : ExposedSQLException) : CreatePlacemarkError(cause = null)
    class Unexpected(override val cause: Throwable) : CreatePlacemarkError(cause)
}