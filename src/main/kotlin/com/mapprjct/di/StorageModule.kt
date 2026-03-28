package com.mapprjct.di

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.client.LogMode
import aws.smithy.kotlin.runtime.net.url.Url
import com.mapprjct.AppConfig
import com.mapprjct.com.mapprjct.database.storage.impl.S3PlacemarkIconStorage
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.database.storage.PlacemarkIconStorage
import com.mapprjct.database.storage.impl.FileAvatarStorage
import com.mapprjct.database.storage.impl.PostgresSessionStorage

import io.ktor.server.sessions.SessionStorage
import org.koin.dsl.module

val storageModule = module {

    single<S3Client> {
        val appConfig = get<AppConfig>()
        S3Client {
            logMode = LogMode.LogRequest + LogMode.LogResponse
            region = "us-east-1"
            endpointUrl = Url.parse(appConfig.minio.endpoint)
            forcePathStyle = true
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = appConfig.minio.accessKey
                secretAccessKey = appConfig.minio.secretKey
            }
        }
    }
    single<SessionStorage> { PostgresSessionStorage(get()) }
    single<AvatarStorage> { FileAvatarStorage(get()) }
    single<PlacemarkIconStorage> { S3PlacemarkIconStorage(get(),get()) }
}