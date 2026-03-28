package com.mapprjct.di

import com.mapprjct.AppConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module

val databaseModule = module {
    single<Database> {
        val appConfig = getKoin().get<AppConfig>()
        Database.connect(
            url = appConfig.database.url,
            driver = "org.postgresql.Driver",
            user = appConfig.database.username,
            password = appConfig.database.password,
        )
    }
}