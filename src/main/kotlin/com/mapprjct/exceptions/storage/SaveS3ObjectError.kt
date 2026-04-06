package com.mapprjct.com.mapprjct.exceptions.storage

import aws.sdk.kotlin.services.s3.model.S3Exception
import com.mapprjct.exceptions.storage.SavePlacemarkIconError

sealed interface SaveS3ObjectError {
    data class Unexpected(val cause: Throwable) : SaveS3ObjectError
    data class S3StorageUnavailable(val cause: S3Exception) : SaveS3ObjectError
}
