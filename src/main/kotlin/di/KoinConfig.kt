package com.mapprjct.di

import com.mapprjct.database.dao.InvitationRepository
import com.mapprjct.database.dao.ProjectRepository
import com.mapprjct.database.dao.SessionRepository
import com.mapprjct.database.dao.UserRepository
import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.daoimpl.ProjectRepositoryImpl
import com.mapprjct.database.daoimpl.SessionRepositoryImpl
import com.mapprjct.database.daoimpl.UserRepositoryImpl
import com.mapprjct.database.storage.PostgresSessionStorage
import com.mapprjct.repository.ProjectService
import com.mapprjct.repository.UserService
import io.ktor.server.application.*
import io.ktor.server.sessions.SessionStorage
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        koin.setProperty("postgres.url", environment.config.property("postgres.url").getString())
        koin.setProperty("postgres.user", environment.config.property("postgres.user").getString())
        koin.setProperty("postgres.password", environment.config.property("postgres.password").getString())
        slf4jLogger()
        modules(databaseModule,
            module {
                single<SessionStorage> { PostgresSessionStorage(get()) }

                single<UserService> { UserService(get()) }
                single<ProjectService> { ProjectService(get(),get()) }

                single<UserRepository> { UserRepositoryImpl(get()) }
                single<SessionRepository> { SessionRepositoryImpl(get()) }
                single<ProjectRepository> { ProjectRepositoryImpl(get()) }
                single<InvitationRepository> { InvitationRepositoryImpl(get()) }
            }
        )
    }
}
