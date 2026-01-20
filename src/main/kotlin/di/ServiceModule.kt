package com.mapprjct.di

import com.mapprjct.service.InvitationService
import com.mapprjct.service.ProjectService
import com.mapprjct.service.UserService
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module

val serviceModule = module {
    single<UserService> { UserService(get(), get(), get(), get()) }
    single<ProjectService> { ProjectService(get(),get(), get(), get()) }
    single<InvitationService> { InvitationService(get(), get()) }
}