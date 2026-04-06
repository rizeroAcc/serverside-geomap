package com.mapprjct.com.mapprjct.exceptions.storage

import aws.sdk.kotlin.services.s3.model.S3Exception

sealed interface GetS3ObjectError {
    data object NoIconWithSuchKey : GetS3ObjectError
    data class S3StorageError(val cause: S3Exception) : GetS3ObjectError
    data class Unexpected(val cause: Throwable) : GetS3ObjectError
}
