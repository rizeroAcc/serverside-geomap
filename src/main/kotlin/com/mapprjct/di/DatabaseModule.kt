package com.mapprjct.di

import com.mapprjct.AppConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module

val databaseModule = module {
    single<Database> {
        val appConfig = getKoin().get<AppConfig>()
        Database.connect(
            url = appConfig.databaseURL,
            driver = "org.postgresql.Driver",
            user = appConfig.databaseUsername,
            password = appConfig.databasePassword,
        )
    }
}