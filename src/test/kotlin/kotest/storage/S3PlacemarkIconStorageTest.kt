package com.mapprjct.kotest.storage

import arrow.core.right
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.getBucketVersioning
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.BucketVersioningStatus
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.net.url.Url
import com.mapprjct.com.mapprjct.database.storage.impl.S3PlacemarkIconStorage
import com.mapprjct.getTestResourceAsChannel
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.MinIOContainer


class S3PlacemarkIconStorageTest : FunSpec() {
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
        val storage = S3PlacemarkIconStorage(
            client = s3Client,
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

        context("Put icon"){
            test("should put icon"){
                val iconBytes = getTestResourceAsChannel("avatar/AppLogo.png")
                storage.saveIcon("test.png"){
                    iconBytes
                }
                s3Client.getObject(
                    GetObjectRequest {
                        key = "test.png"
                        bucket = bucketName
                    }
                ){ response ->
                    response.body shouldNotBeNull {
                        toByteArray() shouldBe getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
                    }
                }
            }
            test("should replace icon"){
                //todo проверить что нормально все по версиям
                val iconBytes = getTestResourceAsChannel("avatar/AppLogo.png")
                val firstKey = storage.saveIcon("test.png"){
                    iconBytes
                }.getOrNull()!!
                val secondKey = storage.saveIcon("test.png"){
                    getTestResourceAsChannel("avatar/red-locator.png")
                }.getOrNull()!!
                s3Client.getObject(
                    GetObjectRequest {
                        key = firstKey.substringBefore("?v=")
                        versionId = firstKey.substringAfter("?v=")
                        bucket = bucketName
                    }
                ){ response ->
                    response.body shouldNotBeNull {
                        toByteArray() shouldBe getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
                    }
                }
                s3Client.getObject(
                    GetObjectRequest {
                        key = secondKey.substringBefore("?v=")
                        versionId = secondKey.substringAfter("?v=")
                        bucket = bucketName
                    }
                ){ response ->
                    response.body shouldNotBeNull {
                        toByteArray() shouldBe getTestResourceAsChannel("avatar/red-locator.png").toByteArray()
                    }
                }
            }
        }
    }
}