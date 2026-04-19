package com.mapprjct.exceptions.domain.placemark

import aws.sdk.kotlin.services.s3.model.S3Exception
import com.mapprjct.model.dto.PlacemarkDTO
import kotlinx.io.IOException
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface UpdatePlacemarkIconError {
    data class NotFound(val placemarkID: String) : UpdatePlacemarkIconError
    data class UserNotStayInProject(val projectID: String) : UpdatePlacemarkIconError
    data class NoPermissionToUpdatePlacemark(val projectID: String) : UpdatePlacemarkIconError
    data class VersionConflict(val newPlacemarkDTO: PlacemarkDTO) : UpdatePlacemarkIconError
    data class InvalidIconFormat(val allowedExtensions : List<String>) : UpdatePlacemarkIconError

    data class FileStorage(val cause : S3Exception) : UpdatePlacemarkIconError
    data object ConcurrentUpdate : UpdatePlacemarkIconError

    data class Database(val cause: ExposedSQLException) : UpdatePlacemarkIconError
    data class Unexpected(val cause: Throwable) : UpdatePlacemarkIconError
}