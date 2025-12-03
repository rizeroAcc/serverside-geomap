package com.mapprjct.feature.authentication.authorization

import com.mapprjct.APISession
import com.mapprjct.feature.authentication.registration.RegistrationRequest
import com.mapprjct.feature.authentication.registration.toUserDto
import com.mapprjct.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import kotlinx.datetime.Clock
import org.koin.ktor.ext.inject
import kotlin.getValue
import kotlin.time.Duration

fun Application.configureAuthorizationRouting() {
    val userRepository: UserRepository by inject()
    routing{
        post("/signin") {
            val request = call.receive<SignInRequest>()
            if (userRepository.isAuthorizationValid(request.toUserDTO())){
                val session = APISession(
                    phone = request.phone,
                    expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*168 //7 days
                )
                call.sessions.set("Authorization",session)
                call.respond(HttpStatusCode.OK, "Signed in successfully")
            }else{
                call.respond(HttpStatusCode.Unauthorized, "Invalid authorization")
            }
        }
    }
}