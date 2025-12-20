package com.mapprjct.app

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.configurePlugins() {
    install(ContentNegotiation) {
        json()
    }
    install(PartialContent) {
        maxRangeCount = 10
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(DefaultHeaders) {
        //header("X-Engine", "Ktor")
    }
}
