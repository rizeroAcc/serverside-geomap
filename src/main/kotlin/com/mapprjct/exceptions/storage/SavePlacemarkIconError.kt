package com.mapprjct.exceptions.storage

import aws.sdk.kotlin.services.s3.model.S3Exception
import io.minio.errors.MinioException
import kotlinx.io.IOException

sealed interface SavePlacemarkIconError {
    data class Unexpected(val cause: Throwable) : SavePlacemarkIconError
    data class S3StorageUnavailable(val cause: S3Exception) : SavePlacemarkIconError
}
