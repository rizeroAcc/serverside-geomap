package com.mapprjct.exceptions.storage.s3

import aws.sdk.kotlin.services.s3.model.S3Exception

sealed interface DeleteS3ObjectsError {
    val errorMessage: String
    data class KeyParseError(override val errorMessage : String) : DeleteS3ObjectsError
    data class S3StorageError(val cause: S3Exception) : DeleteS3ObjectsError{
        override val errorMessage: String = cause.message
    }
    data class Unexpected(val cause: Throwable) : DeleteS3ObjectsError{
        override val errorMessage: String = "Unknown exception"
    }
}