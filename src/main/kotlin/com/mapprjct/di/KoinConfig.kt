package com.mapprjct.di

import com.mapprjct.AppConfig
import com.mapprjct.ApplicationStartMode
import com.mapprjct.DatabaseConfig
import com.mapprjct.MinioConfig
import com.mapprjct.com.mapprjct.utils.SuspendTransactionProvider
import com.mapprjct.com.mapprjct.utils.TransactionProvider
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin(startMode: ApplicationStartMode) {
    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single<AppConfig>(createdAtStart = true) {
                    when(startMode) {
                        is ApplicationStartMode.TEST -> {
                            AppConfig(
                                database = startMode.dbConfig,
                                minio = startMode.minioConfig,
                                avatarResourcePath = environment.config.property("resource.avatar.path").getString(),
                                placemarkIconsPath = environment.config.property("resource.placemark-icons.path").getString(),
                            )
                        }
                        else -> {
                            AppConfig(
                                database = DatabaseConfig(
                                    url = environment.config.property("postgres.url").getString(),
                                    username = environment.config.property("postgres.user").getString(),
                                    password = environment.config.property("postgres.password").getString()
                                ),
                                minio = MinioConfig(
                                    endpoint = environment.config.property("minio.endpoint").getString(),
                                    accessKey = environment.config.property("minio.accessKey").getString(),
                                    secretKey = environment.config.property("minio.secretKey").getString(),
                                    placemarkIconBucketName = environment.config.property("minio.placemark-icon-bucket.name").getString(),
                                ),
                                avatarResourcePath = environment.config.property("resource.avatar.path").getString(),
                                placemarkIconsPath = environment.config.property("resource.placemark-icons.path").getString()
                            )
                        }
                    }
                }
                single<TransactionProvider> { SuspendTransactionProvider(get<Database>()) }
                single<CoroutineScope>(named("Main background scope")) { CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("Main background scope")) }
            },
            databaseModule,
            repositoryModule,
            storageModule,
            serviceModule,
        ).createEagerInstances()
    }
}
