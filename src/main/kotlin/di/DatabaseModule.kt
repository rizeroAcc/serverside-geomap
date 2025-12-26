package com.mapprjct.di

import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module

val databaseModule = module {
    single<Database> {
        Database.connect(
            url = getKoin().getProperty("postgres.url")!!,
            driver = "org.postgresql.Driver",
            user = getKoin().getProperty("postgres.user")!!,
            password = getKoin().getProperty("postgres.password")!!,
        )
    }
}