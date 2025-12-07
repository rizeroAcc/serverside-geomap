package com.mapprjct

import com.mapprjct.app.configureDBTables
import com.mapprjct.app.configureHTTP
import com.mapprjct.app.configureMonitoring
import com.mapprjct.app.configureSecurity
import com.mapprjct.app.configureSerialization
import com.mapprjct.di.configureKoin
import com.mapprjct.controller.configureAuthenticationRouting
import com.mapprjct.controller.configureProfileController
import com.mapprjct.controller.configureProjects
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureStaticContent()
    configureAuthenticationRouting()
    configureProjects()
    configureProfileController()
    configureDBTables()
}
