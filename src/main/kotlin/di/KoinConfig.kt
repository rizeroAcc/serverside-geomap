package com.mapprjct.di

import com.mapprjct.database.dao.SessionDAO
import com.mapprjct.database.dao.UserDAO
import com.mapprjct.database.sessions.SessionDAOImpl
import com.mapprjct.database.users.UserDAOImpl
import com.mapprjct.feature.authentication.sessions.PostgresSessionStorage
import com.mapprjct.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.sessions.SessionStorage
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(databaseModule,
            module {
                single<UserRepository> { UserRepository(get()) }
                single<UserDAO> { UserDAOImpl(get()) }
                single<SessionDAO> { SessionDAOImpl(get()) }
                single<SessionStorage> { PostgresSessionStorage(get()) }
            }
        )
    }
}
