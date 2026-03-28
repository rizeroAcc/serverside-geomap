package com.mapprjct.exceptions.storage

import io.minio.errors.MinioException
import kotlinx.io.IOException

sealed interface SaveOrUpdatePlacemarkIconError {
    data class Unexpected(val cause: Throwable) : SaveOrUpdatePlacemarkIconError
    data class IO(val cause: IOException) : SaveOrUpdatePlacemarkIconError
    data class Minio(val cause: MinioException) : SaveOrUpdatePlacemarkIconError
}
