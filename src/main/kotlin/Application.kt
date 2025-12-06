package com.mapprjct

import com.mapprjct.di.configureKoin
import com.mapprjct.feature.authentication.configureAuthenticationRouting
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureAuthenticationRouting()
    configureDBTables()
}
