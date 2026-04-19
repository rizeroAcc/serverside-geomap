package com.mapprjct.di

import com.mapprjct.service.InvitationService
import com.mapprjct.service.PlacemarkService
import com.mapprjct.service.ProjectService
import com.mapprjct.service.UserService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val serviceModule = module {
    single<UserService>(createdAtStart = true) { UserService(get(), get(), get()) }
    single<ProjectService>(createdAtStart = true) { ProjectService(get(),get(), get(), get()) }
    single<InvitationService>(createdAtStart = true) { InvitationService(get(), get(), get()) }
    single<PlacemarkService>(createdAtStart = true) {
        PlacemarkService(get(),
            get(),
            get(),
            get(named("PlacemarkIconS3Storage")))
    }
}