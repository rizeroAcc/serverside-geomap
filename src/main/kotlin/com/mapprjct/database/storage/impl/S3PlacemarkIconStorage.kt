package com.mapprjct.com.mapprjct.database.storage.impl

import arrow.core.Either
import arrow.core.raise.either
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.content.ByteStream
import com.mapprjct.com.mapprjct.database.storage.AbstractS3VersioningObjectStorage
import com.mapprjct.com.mapprjct.exceptions.storage.GetPlacemarkIconError
import com.mapprjct.com.mapprjct.exceptions.storage.GetS3ObjectError
import com.mapprjct.com.mapprjct.exceptions.storage.SaveS3ObjectError
import com.mapprjct.database.storage.PlacemarkIconStorage
import com.mapprjct.exceptions.storage.SavePlacemarkIconError
import io.ktor.utils.io.*

class S3PlacemarkIconStorage(
    client: S3Client,
    bucketName: String,
) : PlacemarkIconStorage, AbstractS3VersioningObjectStorage(client, bucketName){
    override suspend fun saveIcon(
        iconKey: String,
        iconBytesProvider: suspend () -> ByteReadChannel
    ): Either<SavePlacemarkIconError, String> = either {
        saveObject(iconKey,iconBytesProvider).mapLeft { error ->
            when(error){
                is SaveS3ObjectError.S3StorageUnavailable -> SavePlacemarkIconError.S3StorageUnavailable(error.cause)
                is SaveS3ObjectError.Unexpected -> SavePlacemarkIconError.Unexpected(error.cause)
            }
        }.bind()
    }

    override suspend fun getIcon(iconKey : String): Either<GetPlacemarkIconError, ByteStream> = either {
        getObject(iconKey).mapLeft { error ->
            when(error){
                GetS3ObjectError.NoIconWithSuchKey -> GetPlacemarkIconError.NoIconWithSuchKey
                is GetS3ObjectError.S3StorageError -> GetPlacemarkIconError.S3StorageError(error.cause)
                is GetS3ObjectError.Unexpected -> GetPlacemarkIconError.Unexpected(error.cause)
            }
        }.bind()
    }
}