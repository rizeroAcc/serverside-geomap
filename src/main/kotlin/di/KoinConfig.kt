package com.mapprjct.di

import com.mapprjct.database.dao.SessionDAO
import com.mapprjct.database.dao.UserDAO
import com.mapprjct.database.daoimpl.ProjectDAOImpl
import com.mapprjct.database.daoimpl.SessionDAOImpl
import com.mapprjct.database.daoimpl.UserDAOImpl
import com.mapprjct.database.storage.PostgresSessionStorage
import com.mapprjct.repository.UserRepository
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
                single<UserRepository> { UserRepository(get()) }
                single<UserDAO> { UserDAOImpl(get()) }
                single<SessionDAO> { SessionDAOImpl(get()) }
                single<SessionStorage> { PostgresSessionStorage(get()) }
                single<ProjectDAOImpl> { ProjectDAOImpl(get()) }
            }
        )
    }
}
