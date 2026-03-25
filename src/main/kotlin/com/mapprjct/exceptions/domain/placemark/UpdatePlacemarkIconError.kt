package com.mapprjct.exceptions.domain.placemark

import com.mapprjct.model.dto.PlacemarkDTO

sealed interface UpdatePlacemarkIconError {
    data class NotFound(val placemarkID: String) : UpdatePlacemarkIconError
    data class UserNotStayInProject(val projectID: String) : UpdatePlacemarkIconError
    data class NoPermissionToUpdatePlacemark(val projectID: String) : UpdatePlacemarkIconError
    data class VersionConflict(val newPlacemarkDTO: PlacemarkDTO) : UpdatePlacemarkIconError
    data class InvalidIconFormat(val allowedExtensions : List<String>) : UpdatePlacemarkIconError
}