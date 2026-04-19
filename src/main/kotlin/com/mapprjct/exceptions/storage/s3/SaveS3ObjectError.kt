package com.mapprjct.exceptions.storage.s3

import aws.sdk.kotlin.services.s3.model.S3Exception

sealed interface SaveS3ObjectError {
    val errorMessage: String
    data class Unexpected(val cause: Throwable) : SaveS3ObjectError {
        override val errorMessage: String = cause.message ?: "Unknown error"
    }
    data class S3StorageUnavailable(val cause: S3Exception) : SaveS3ObjectError{
        override val errorMessage: String = cause.message
    }
}