package com.mapprjct.com.mapprjct.exceptions.storage

import aws.sdk.kotlin.services.s3.model.S3Exception
import io.minio.errors.MinioException

sealed interface GetPlacemarkIconError {
    data object NoIconWithSuchKey : GetPlacemarkIconError
    data class S3StorageError(val cause: S3Exception) : GetPlacemarkIconError
    data class Unexpected(val cause: Throwable) : GetPlacemarkIconError
}
