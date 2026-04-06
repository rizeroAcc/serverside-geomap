package com.mapprjct.com.mapprjct.database.storage

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.model.BucketVersioningStatus
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest.Companion.invoke
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.putBucketVersioning
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import com.mapprjct.com.mapprjct.database.storage.impl.S3PlacemarkIconStorage
import com.mapprjct.com.mapprjct.exceptions.storage.GetPlacemarkIconError
import com.mapprjct.com.mapprjct.exceptions.storage.GetS3ObjectError
import com.mapprjct.com.mapprjct.exceptions.storage.SaveS3ObjectError
import com.mapprjct.exceptions.storage.SavePlacemarkIconError
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

abstract class AbstractS3VersioningObjectStorage(
    val s3Client: S3Client,
    val bucketName: String,
) {

    val logger = LoggerFactory.getLogger(S3PlacemarkIconStorage::class.java)!!

    private suspend fun initBucket() {
        catch({
            s3Client.headBucket{
                bucket = bucketName
            }
            logger.debug("Bucket $bucketName found")
        }){ error : S3Exception ->
            when (error) {
                is NotFound -> {
                    catch({
                        s3Client.createBucket {
                            bucket = bucketName
                        }
                        logger.info("Created $bucketName bucket")
                    }){ error : S3Exception ->
                        throw RuntimeException("Unable to start Application. S3 service unavailable: ${error.message}")
                    }
                }
                else -> throw RuntimeException("Unable to start Application. S3 service unavailable: ${error.message}")
            }
        }
    }

    private suspend fun enableBucketVersioning(){
        catch({
            s3Client.putBucketVersioning {
                bucket = bucketName
                versioningConfiguration {
                    status = BucketVersioningStatus.Enabled
                }
            }
        }){
            throw RuntimeException("Unable to start Application. S3 service unavailable: ${it.message}")
        }
    }

    init {
        runBlocking {
            initBucket()
            enableBucketVersioning()
        }
    }

    suspend fun saveObject(
        objectKey: String,
        objectBytesProvider: suspend () -> ByteReadChannel
    ): Either<SaveS3ObjectError, String> = either {
        catch({
            val objectContent = objectBytesProvider().toInputStream().asByteStream()
            val versionID = s3Client.putObject {
                this.bucket = bucketName
                this.key = objectKey
                this.body = objectContent
            }.versionId!!
            "$objectKey?v=$versionID"//Key for this image version
        }){ error: Throwable->
            if (error is S3Exception){
                raise(SaveS3ObjectError.S3StorageUnavailable(error))
            }else{
                raise(SaveS3ObjectError.Unexpected(error))
            }
        }
    }

    suspend fun getObject(objectKey : String): Either<GetS3ObjectError, ByteStream> = either {
        val (iconName,version) = parseKey(objectKey)
        catch({
            val request = GetObjectRequest{
                bucket = bucketName
                key = iconName
                versionId = version
            }
            s3Client.getObject(request){ response ->
                ensureNotNull(response.body){
                    GetS3ObjectError.NoIconWithSuchKey
                }
            }
        }){ error : Throwable->
            when(error){
                is NotFound -> raise(GetS3ObjectError.NoIconWithSuchKey)
                is S3Exception -> raise(GetS3ObjectError.S3StorageError(error))
                else -> raise(GetS3ObjectError.Unexpected(error))
            }
        }
    }

    private fun parseKey(key : String) : Pair<String, String> {
        val splitKey = key.split("?v=")
        if(splitKey.size != 2){
            throw IllegalArgumentException("Key $key does not match pattern objectName.ext?v=versionID")
        }
        return Pair(splitKey[0], splitKey[1])
    }
}