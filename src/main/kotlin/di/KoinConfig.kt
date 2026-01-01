package com.mapprjct.di

import com.mapprjct.ApplicationStartMode
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.SessionRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.daoimpl.SessionRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.storage.PostgresSessionStorage
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
        when(startMode) {
            is ApplicationStartMode.TEST -> {
                koin.setProperty("postgres.url", startMode.dbURL)
                koin.setProperty("postgres.user", startMode.dbUsername)
                koin.setProperty("postgres.password", startMode.dbPassword)
            }
            else -> {
                koin.setProperty("postgres.url", environment.config.property("postgres.url").getString())
                koin.setProperty("postgres.user", environment.config.property("postgres.user").getString())
                koin.setProperty("postgres.password", environment.config.property("postgres.password").getString())
            }
        }
        slf4jLogger()
        modules(databaseModule,
            module {
                single<SessionStorage> { PostgresSessionStorage(get()) }

                single<UserService> { UserService(get(), get<Database>()) }
                single<ProjectService> { ProjectService(get(),get()) }

                single<UserRepository> { UserRepositoryImpl(get()) }
                single<SessionRepository> { SessionRepositoryImpl(get()) }
                single<ProjectRepository> { ProjectRepositoryImpl(get()) }
                single<InvitationRepository> { InvitationRepositoryImpl(get()) }
            }
        )
    }
}
