package com.mapprjct.exceptions.storage.s3

import aws.sdk.kotlin.services.s3.model.S3Exception

sealed interface DeleteS3ObjectError {
    val errorMessage: String
    class KeyParseError(override val errorMessage : String) : DeleteS3ObjectError
    class S3StorageError(val cause : S3Exception) : DeleteS3ObjectError{
        override val errorMessage: String = cause.message
    }
    class Unexpected(val cause : Throwable) : DeleteS3ObjectError {
        override val errorMessage: String = cause.message ?: "Unexpected error"
    }
}