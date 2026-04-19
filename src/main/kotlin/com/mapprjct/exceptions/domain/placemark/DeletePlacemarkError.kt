package com.mapprjct.exceptions.domain.placemark

import java.util.UUID

sealed interface DeletePlacemarkError {
    data class UserNotStayInProject(val projectID: String) : DeletePlacemarkError
    data class NoPermissionToDeletePlacemark(val projectID: String) : DeletePlacemarkError
}
