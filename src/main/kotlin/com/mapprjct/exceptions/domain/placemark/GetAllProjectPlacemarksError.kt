package com.mapprjct.com.mapprjct.exceptions.domain.placemark

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.util.UUID

sealed interface GetAllProjectPlacemarksError {
    data class ProjectNotFound(val projectID: String) : GetAllProjectPlacemarksError
    data class UserNotStayInProject(val projectID: String) : GetAllProjectPlacemarksError
    data class Database(val exception: ExposedSQLException) : GetAllProjectPlacemarksError
    data class Unexpected(val exception: Throwable) : GetAllProjectPlacemarksError
}
