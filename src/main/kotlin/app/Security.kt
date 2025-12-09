package com.mapprjct.app

import com.mapprjct.dto.APISession
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.response.respond
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.header
import org.koin.ktor.ext.inject


fun Application.configureSecurity() {
    val pgSessionStorage : SessionStorage by inject()
    install(Sessions) {
        header<APISession>("Authorization", storage = pgSessionStorage) {

        }
    }
    install(Authentication) {
        session<APISession>("auth-session"){
            validate { session ->
                if (session.expireAt > System.currentTimeMillis()){
                    session
                }else{
                    null
                }
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, message = "Token invalid or expired")
            }
        }
    }
}
