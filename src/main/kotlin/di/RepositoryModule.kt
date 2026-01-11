package com.mapprjct.di

import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.daoimpl.SessionRepositoryImpl
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.SessionRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    single<ProjectRepository> { ProjectRepositoryImpl(get()) }
    single<InvitationRepository> { InvitationRepositoryImpl(get()) }
}