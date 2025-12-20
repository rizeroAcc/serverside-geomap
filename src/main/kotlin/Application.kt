package com.mapprjct

import com.mapprjct.app.configureDBTables
import com.mapprjct.app.configureSecurity
import com.mapprjct.app.configurePlugins
import com.mapprjct.di.configureKoin
import com.mapprjct.controller.configureAuthenticationController
import com.mapprjct.controller.configureProfileController
import com.mapprjct.controller.configureProjectsController
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)

}

fun Application.module() {
    configureKoin()
    configureSecurity()
    configurePlugins()
    configureAuthenticationController()
    configureProjectsController()
    configureProfileController()
    configureDBTables()
}
