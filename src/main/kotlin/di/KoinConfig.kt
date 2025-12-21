package com.mapprjct.di

import com.mapprjct.database.dao.InvitationDAO
import com.mapprjct.database.dao.ProjectDAO
import com.mapprjct.database.dao.SessionDAO
import com.mapprjct.database.dao.UserDAO
import com.mapprjct.database.daoimpl.InvitationDAOImpl
import com.mapprjct.database.daoimpl.ProjectDAOImpl
import com.mapprjct.database.daoimpl.SessionDAOImpl
import com.mapprjct.database.daoimpl.UserDAOImpl
import com.mapprjct.database.storage.PostgresSessionStorage
import com.mapprjct.repository.ProjectRepository
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
                single<SessionStorage> { PostgresSessionStorage(get()) }

                single<UserRepository> { UserRepository(get()) }
                single<ProjectRepository> { ProjectRepository(get(),get()) }

                single<UserDAO> { UserDAOImpl(get()) }
                single<SessionDAO> { SessionDAOImpl(get()) }
                single<ProjectDAO> { ProjectDAOImpl(get()) }
                single<InvitationDAO> { InvitationDAOImpl(get()) }
            }
        )
    }
}
