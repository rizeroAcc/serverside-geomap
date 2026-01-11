package com.mapprjct

import com.mapprjct.di.repositoryModule
import com.mapprjct.di.serviceModule
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(database: Database, appConfig: AppConfig) : KoinApplication{
    return startKoin {
        modules(
            module{
                single { appConfig }
                single { database }
            },
            repositoryModule,
            serviceModule,
        )
    }
}