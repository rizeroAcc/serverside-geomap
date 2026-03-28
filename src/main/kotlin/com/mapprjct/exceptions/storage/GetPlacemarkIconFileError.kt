package com.mapprjct.com.mapprjct.exceptions.storage

import io.minio.errors.MinioException

sealed interface GetPlacemarkIconFileError {
    data class Minio(val exception : MinioException) : GetPlacemarkIconFileError
    data object PlacemarkDoesNotHaveIcon : GetPlacemarkIconFileError
}
