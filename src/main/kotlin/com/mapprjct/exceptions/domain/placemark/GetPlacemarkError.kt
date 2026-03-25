package com.mapprjct.com.mapprjct.exceptions.domain.placemark

import com.mapprjct.model.datatype.StringUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

sealed interface GetPlacemarkError {
    data class NotFound(val placemarkID : String) : GetPlacemarkError
    data class Database(val exception: ExposedSQLException) : GetPlacemarkError
    data class Unexpected(val exception: Throwable) : GetPlacemarkError
}
