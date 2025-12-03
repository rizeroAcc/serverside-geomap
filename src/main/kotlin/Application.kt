package com.mapprjct

import com.mapprjct.di.configureKoin
import com.mapprjct.feature.authentication.registration.configureRegistrationRouting
import com.mapprjct.feature.authentication.sessions.PostgresSessionStorage
import io.ktor.server.application.*
import org.koin.ktor.ext.getKoin

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    this.monitor.subscribe(ApplicationStarted) {
        val koin = getKoin()
        koin.setProperty("postgres.url", environment.config.property("postgres.url").getString())
        koin.setProperty("postgres.user", environment.config.property("postgres.user").getString())
        koin.setProperty("postgres.password", environment.config.property("postgres.password").getString())
    }
    configureKoin()

    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRegistrationRouting()
}
