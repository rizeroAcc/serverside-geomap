package com.mapprjct.exceptions.storage.s3

import aws.sdk.kotlin.services.s3.model.S3Exception

sealed interface GetS3ObjectError {
    val errorMessage: String
    class KeyParseError(override val errorMessage : String) : GetS3ObjectError
    data object NoObjectWithSuchKey : GetS3ObjectError {
        override val errorMessage: String
            get() = "No object with such key`"
    }
    data class S3StorageError(val cause: S3Exception) : GetS3ObjectError {
        override val errorMessage: String = cause.message
    }
    data class Unexpected(val cause: Throwable) : GetS3ObjectError{
        override val errorMessage: String
            get() = cause.message ?: "Unknown error"
    }
}