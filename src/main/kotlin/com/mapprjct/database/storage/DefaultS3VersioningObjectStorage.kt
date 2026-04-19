package com.mapprjct.database.storage

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.deleteObjects
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.model.BucketVersioningStatus
import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.putBucketVersioning
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import com.mapprjct.database.storage.S3VersioningObjectStorage
import com.mapprjct.exceptions.storage.s3.DeleteS3ObjectError
import com.mapprjct.exceptions.storage.s3.DeleteS3ObjectsError
import com.mapprjct.exceptions.storage.s3.GetS3ObjectError
import com.mapprjct.com.mapprjct.exceptions.storage.ObjectKeyParseError
import com.mapprjct.exceptions.storage.s3.SaveS3ObjectError
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class DefaultS3VersioningObjectStorage(
    val s3Client: S3Client,
    val bucketName: String,
) : S3VersioningObjectStorage {
    val logger = LoggerFactory.getLogger(DefaultS3VersioningObjectStorage::class.java)!!
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

    override suspend fun saveObject(
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
            "$objectKey?v=$versionID"//Key for this object version
        }){ error: Throwable->
            if (error is S3Exception){
                raise(SaveS3ObjectError.S3StorageUnavailable(error))
            }else{
                raise(SaveS3ObjectError.Unexpected(error))
            }
        }
    }

    override suspend fun getObject(objectKey : String): Either<GetS3ObjectError, ByteArray> = either {
        val (objectName,version) = parseKey(objectKey).mapLeft {
            GetS3ObjectError.KeyParseError(it.message)
        }.bind()
        catch({
            val request = GetObjectRequest{
                bucket = bucketName
                key = objectName
                versionId = version
            }
            s3Client.getObject(request){ response ->
                ensureNotNull(response.body){
                    GetS3ObjectError.NoObjectWithSuchKey
                }.toByteArray()
            }
        }){ error : Throwable->
            when(error){
                is S3Exception -> {
                    when(error.sdkErrorMetadata.errorCode){
                        "NoSuchVersion" -> raise(GetS3ObjectError.NoObjectWithSuchKey)
                        else -> raise(GetS3ObjectError.S3StorageError(error))
                    }
                }
                else -> raise(GetS3ObjectError.Unexpected(error))
            }
        }
    }

    override suspend fun deleteObject(objectKey: String) : Either<DeleteS3ObjectError,Unit> = either {
        val (objectName, version) = parseKey(objectKey).mapLeft {
            DeleteS3ObjectError.KeyParseError(it.message)
        }.bind()
        catch({
            s3Client.deleteObject {
                bucket = bucketName
                key = objectName
                versionId = version
            }
        }){ error->
            when(error){
                is S3Exception -> raise(DeleteS3ObjectError.S3StorageError(error))
                else -> raise(DeleteS3ObjectError.Unexpected(error))
            }
        }
    }
    override suspend fun deleteObjects(objectKeys : List<String>) : Either<DeleteS3ObjectsError,Unit> = either {
        val objectIdentifiers = objectKeys.map {
            val (name,version) = parseKey(it).mapLeft { error ->
                DeleteS3ObjectsError.KeyParseError(error.message)
            }.bind()
            ObjectIdentifier{
                key = name
                versionId = version
            }
        }
        catch({
            s3Client.deleteObjects{
                bucket = bucketName
                delete = Delete {
                    objects = objectIdentifiers
                }
            }
        }){ error->
            when(error){
                is S3Exception -> raise(DeleteS3ObjectsError.S3StorageError(error))
                else -> raise(DeleteS3ObjectsError.Unexpected(error))
            }
        }
    }
    private fun parseKey(key : String) : Either<ObjectKeyParseError,Pair<String, String>> = either {
        val splitKey = key.split("?v=")
        if(splitKey.size != 2){
            raise(ObjectKeyParseError("Key $key does not match pattern objectName.ext?v=versionID"))
        }
        Pair(splitKey[0], splitKey[1])
    }
}