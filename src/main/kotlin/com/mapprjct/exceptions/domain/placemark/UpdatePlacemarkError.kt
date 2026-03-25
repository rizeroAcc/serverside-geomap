package com.mapprjct.com.mapprjct.exceptions.domain.placemark

import com.mapprjct.model.dto.PlacemarkDTO
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface UpdatePlacemarkError {
    data object IDUpdateForbidden : UpdatePlacemarkError
    data object ProjectIDUpdateForbidden : UpdatePlacemarkError
    data object BlankName : UpdatePlacemarkError

    data class UserNotStayInProject(val projectID: String) : UpdatePlacemarkError
    data class NoPermissionToUpdatePlacemark(val projectID: String) : UpdatePlacemarkError

    data class VersionConflict(val newPlacemarkDTO : PlacemarkDTO) : UpdatePlacemarkError
    data object ConcurrentUpdate : UpdatePlacemarkError

    data class NotFound(val id: String) : UpdatePlacemarkError

    data class Database(val exception : ExposedSQLException) : UpdatePlacemarkError
    data class Unexpected(val exception: Throwable) : UpdatePlacemarkError
}
