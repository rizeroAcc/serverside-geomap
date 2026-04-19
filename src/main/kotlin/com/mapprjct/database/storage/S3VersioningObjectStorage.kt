package com.mapprjct.database.storage

import arrow.core.Either
import com.mapprjct.exceptions.storage.s3.DeleteS3ObjectError
import com.mapprjct.exceptions.storage.s3.DeleteS3ObjectsError
import com.mapprjct.exceptions.storage.s3.GetS3ObjectError
import com.mapprjct.exceptions.storage.s3.SaveS3ObjectError
import io.ktor.utils.io.ByteReadChannel

/**
 * Store objects in s3 storage with enabled versioning on bucket.
 * Key must be in format objectName.ext?v=versionID
 * */
interface S3VersioningObjectStorage {
    /**
     * @param objectKey - must be set without version
     * */
    suspend fun saveObject(objectKey: String, objectBytesProvider: suspend () -> ByteReadChannel):
            Either<SaveS3ObjectError, String>
    suspend fun getObject(objectKey : String): Either<GetS3ObjectError, ByteArray>
    suspend fun deleteObject(objectKey: String) : Either<DeleteS3ObjectError,Unit>
    suspend fun deleteObjects(objectKeys : List<String>) : Either<DeleteS3ObjectsError,Unit>
}