package com.mapprjct.kotest.storage

import arrow.core.getOrElse
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.getBucketVersioning
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.BucketVersioningStatus
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.net.url.Url
import com.mapprjct.database.storage.DefaultS3VersioningObjectStorage
import com.mapprjct.database.storage.S3VersioningObjectStorage
import com.mapprjct.exceptions.storage.s3.DeleteS3ObjectError
import com.mapprjct.exceptions.storage.s3.GetS3ObjectError
import com.mapprjct.getTestResourceAsChannel
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.utils.io.*
import org.testcontainers.containers.MinIOContainer
import java.util.UUID


class S3VersioningObjectStorageTest : FunSpec() {
    init {
        val minIOContainer = MinIOContainer("minio/minio:latest")
            .withReuse(true)

        install(TestContainerSpecExtension(minIOContainer))
        val s3Client = S3Client{
            region = "us-east-1"
            endpointUrl = Url.parse(minIOContainer.s3URL)
            forcePathStyle = true
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = minIOContainer.userName
                secretAccessKey = minIOContainer.password
            }
        }
        val bucketName = "test"
        val storage : S3VersioningObjectStorage = DefaultS3VersioningObjectStorage(
            s3Client = s3Client,
            bucketName = bucketName,
        )
        beforeEach {
            s3Client.listObjectsV2{
                bucket = bucketName
            }.contents?.forEach { item ->
                s3Client.deleteObject{
                    key = item.key
                    bucket = bucketName
                }
            }
        }

        suspend fun nativeSaveObject(objectName : String, objectBytes : ()-> ByteStream) : String{
            val objectVersion = s3Client.putObject {
                bucket = bucketName
                key = objectName
                body = objectBytes()
            }.versionId!!
            return "$objectName?v=$objectVersion"
        }

        suspend fun nativeGetObject(objectKey : String) : ByteArray {
            val (objectName, version) = objectKey.split("?v=")
            val request = GetObjectRequest{
                bucket = bucketName
                key = objectName
                versionId = version
            }
            return s3Client.getObject(request){ response ->
                response.body!!.toByteArray()
            }
        }

        context("bucket configuration"){
            test("bucket should exists"){
                s3Client.headBucket{
                    bucket = bucketName
                } shouldNotBeNull{}
            }
            test("versioning should be enabled") {
                val versioning = s3Client.getBucketVersioning { bucket = bucketName }
                versioning.status shouldBe BucketVersioningStatus.Enabled
            }
        }

        context("Get object"){
            val objectName = "testObject.extension"
            val objectBytes = getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
            test("should get existing object"){
                val objectKey = nativeSaveObject(objectName){
                    objectBytes.asByteStream()
                }
                storage.getObject(objectKey) shouldBeRight objectBytes
            }
            test("should return NoObjectWithSuchKey if object does not exist"){
                val key = objectName + "?v=${UUID.randomUUID()}"
                storage.getObject(key) shouldBeLeft GetS3ObjectError.NoObjectWithSuchKey
            }
        }

        context("Save object"){
            test("should save object"){
                val iconBytes = getTestResourceAsChannel("avatar/AppLogo.png")
                val key = storage.saveObject("test.png"){
                    iconBytes
                }.getOrElse { throw RuntimeException("Save object failed. ${it.errorMessage}") }
                storage.getObject(key) shouldBeRight
                        getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
            }
            test("should replace object"){
                val iconBytes = getTestResourceAsChannel("avatar/AppLogo.png")
                val firstKey = storage.saveObject("test.png"){ iconBytes }
                    .getOrElse { throw RuntimeException("Failed to save first object") }
                val secondKey = storage.saveObject("test.png"){
                    getTestResourceAsChannel("avatar/red-locator.png")
                }.getOrElse { throw RuntimeException("Failed to replace object") }
                nativeGetObject(firstKey) shouldBe
                        getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
                nativeGetObject(secondKey) shouldBe
                        getTestResourceAsChannel("avatar/red-locator.png").toByteArray()
            }
        }

        context("Delete object"){
            val objectName = "testObject.extension"
            val objectBytes = getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
            test("should delete existing object"){
                val key = nativeSaveObject(objectName){
                    objectBytes.asByteStream()
                }
                nativeGetObject(key) shouldBe objectBytes
                storage.deleteObject(key) shouldBeRight Unit
                storage.getObject(key) shouldBeLeft GetS3ObjectError.NoObjectWithSuchKey
            }
            test("should delete all specified objects"){
                val keys = buildList {
                    repeat(3){
                        add(nativeSaveObject(objectName){
                            objectBytes.asByteStream()
                        })
                    }
                }
                keys.forEach { nativeGetObject(it) shouldBe objectBytes }
                storage.deleteObjects(keys)
                keys.forEach {
                    storage.getObject(it) shouldBeLeft GetS3ObjectError.NoObjectWithSuchKey
                }
            }
        }
    }
}