package com.mapprjct.com.mapprjct.database.storage.impl

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.model.BucketVersioningStatus
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.putBucketVersioning
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import com.mapprjct.com.mapprjct.exceptions.storage.GetPlacemarkIconError
import com.mapprjct.database.storage.PlacemarkIconStorage
import com.mapprjct.exceptions.storage.SavePlacemarkIconError
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class S3PlacemarkIconStorage(
    val s3Client: S3Client,
    val bucketName: String,
) : PlacemarkIconStorage {
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

    override suspend fun saveIcon(
        iconKey: String,
        iconBytesProvider: suspend () -> ByteReadChannel
    ): Either<SavePlacemarkIconError, String> = either {
        catch({
            val iconContent = iconBytesProvider().toInputStream().asByteStream()
            val versionID = s3Client.putObject {
                this.bucket = bucketName
                this.key = iconKey
                this.body = iconContent
            }.versionId!!
            "$iconKey?v=$versionID"//Key for this image version
        }){ error: Throwable->
            if (error is S3Exception){
                raise(SavePlacemarkIconError.S3StorageUnavailable(error))
            }else{
                raise(SavePlacemarkIconError.Unexpected(error))
            }
        }
    }

    override suspend fun getIcon(iconKey : String): Either<GetPlacemarkIconError, ByteStream> = either {
        val (iconName,version) = parseKey(iconKey)
        catch({
            val request = GetObjectRequest{
                bucket = bucketName
                key = iconName
                versionId = version
            }
            s3Client.getObject(request){ response ->
                ensureNotNull(response.body){
                    GetPlacemarkIconError.NoIconWithSuchKey
                }
            }
        }){ error : Throwable->
            when(error){
                is NotFound -> raise(GetPlacemarkIconError.NoIconWithSuchKey)
                is S3Exception -> raise(GetPlacemarkIconError.S3StorageError(error))
                else -> raise(GetPlacemarkIconError.Unexpected(error))
            }
        }
    }
    private fun parseKey(key : String) : Pair<String, String> {
        val splitKey = key.split("?v=")
        if(splitKey.size != 2){
            throw IllegalArgumentException("Key $key does not match pattern imageName.ext?v=versionID")
        }
        return Pair(splitKey[0], splitKey[1])
    }
}