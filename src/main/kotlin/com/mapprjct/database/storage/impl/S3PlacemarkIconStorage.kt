package com.mapprjct.com.mapprjct.database.storage.impl

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.recover
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.S3Exception
import com.mapprjct.AppConfig
import com.mapprjct.com.mapprjct.exceptions.storage.DeletePlacemarkIconError
import com.mapprjct.com.mapprjct.exceptions.storage.GetPlacemarkIconFileError
import com.mapprjct.database.storage.PlacemarkIconStorage
import com.mapprjct.exceptions.storage.SaveOrUpdatePlacemarkIconError
import com.mapprjct.model.dto.PlacemarkDTO
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import io.minio.*
import io.minio.errors.MinioException
import org.slf4j.LoggerFactory

class S3PlacemarkIconStorage(
    val s3Client: S3Client,
    val appConfig: AppConfig,
) : PlacemarkIconStorage {

    val bucketName = appConfig.minio.placemarkIconBucketName
    val logger = LoggerFactory.getLogger(S3PlacemarkIconStorage::class.java)!!

//    private suspend fun ensureBucketExists() {
//        recover({
//            s3Client.headBucket{
//                bucket = bucketName
//            }
//        }){ error ->
//            when (error) {
//                is S3Exception -> error.sdkErrorMetadata.errorType
//                NotFound -> {
//                    s3Client.createBucket {
//                        bucket = bucketName
//                    }
//                }
//            }
//        }
//
//    }
//
//    init {
//        ensureBucketExists()
//    }

    private fun buildObjectName(placemarkDTO: PlacemarkDTO, fileExtension: String): String {
        return "placemarks/${placemarkDTO.placemarkID.value}/icon.$fileExtension"
    }

    override suspend fun saveIcon(
        placemarkDTO: PlacemarkDTO,
        fileExtension: String,
        iconBytesProvider: suspend () -> ByteReadChannel
    ): Either<SaveOrUpdatePlacemarkIconError, String> = either {
        TODO()
    }

    override suspend fun getPlacemarkIcon(placemarkDTO: PlacemarkDTO): Either<GetPlacemarkIconFileError, ByteReadChannel> = either {
        TODO()
    }

    override suspend fun deletePlacemarkIcon(placemarkDTO: PlacemarkDTO): Either<DeletePlacemarkIconError, Unit> = either {
        TODO()
    }
}