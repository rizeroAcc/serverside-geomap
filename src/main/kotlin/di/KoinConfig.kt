package com.mapprjct.di

import com.mapprjct.AppConfig
import com.mapprjct.ApplicationStartMode
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.SessionRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.daoimpl.SessionRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.storage.impl.PostgresSessionStorage
import com.mapprjct.service.InvitationService
import com.mapprjct.service.ProjectService
import com.mapprjct.service.UserService
import io.ktor.server.application.*
import io.ktor.server.sessions.SessionStorage
import org.jetbrains.exposed.v1.jdbc.Database
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
            },
            databaseModule,
            repositoryModule,
            storageModule,
            serviceModule,

        )
    }
}
