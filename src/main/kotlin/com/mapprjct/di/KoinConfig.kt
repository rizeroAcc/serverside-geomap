package com.mapprjct.di

import com.mapprjct.AppConfig
import com.mapprjct.ApplicationStartMode
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
                single<AppConfig> {
                    when(startMode) {
                        is ApplicationStartMode.TEST -> {
                            AppConfig(
                                databaseURL = startMode.dbURL,
                                databaseUsername = startMode.dbUsername,
                                databasePassword = startMode.dbPassword,
                                avatarResourcePath = environment.config.property("resource.avatar.path").getString()
                            )
                        }
                        else -> {
                            AppConfig(
                                databaseURL = environment.config.property("postgres.url").getString(),
                                databaseUsername = environment.config.property("postgres.user").getString(),
                                databasePassword = environment.config.property("postgres.password").getString(),
                                avatarResourcePath = environment.config.property("resource.avatar.path").getString()
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
        )
    }
}
