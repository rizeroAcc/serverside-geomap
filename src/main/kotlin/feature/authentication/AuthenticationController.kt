package com.mapprjct.feature.authentication

import com.mapprjct.feature.authentication.authorization.SignInRequest
import com.mapprjct.feature.authentication.authorization.toUserDTO
import com.mapprjct.database.sessions.APISession
import com.mapprjct.feature.authentication.authorization.SignInResponse
import com.mapprjct.feature.authentication.registration.RegistrationRequest
import com.mapprjct.feature.authentication.registration.toUserDto
import com.mapprjct.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.pluginRegistry
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.sessions
import kotlinx.datetime.Clock
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.configureAuthenticationRouting() {
    val userRepository: UserRepository by inject()
    val sessionStorage by inject<SessionStorage>()
    routing{
        post("/signin") {
            val request = call.receive<SignInRequest>()
            val user = userRepository.getUserWithCredentials(request.toUserDTO())
            if (user != null){
                val session = APISession(
                    phone = user.phone,
                    expireAt = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 168 //7 days
                )
                call.sessions.set("Authorization",session)
                call.respond(HttpStatusCode.OK, SignInResponse(
                    username = user.username,
                    phone = user.phone,
                ))
            }else{
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }
        post("/logout") {
            val sessionId = call.request.headers["Authorization"]
            if (sessionId != null) {
                sessionStorage.invalidate(sessionId)
                call.respond(HttpStatusCode.OK,"Logged out")
            }
            call.respond(HttpStatusCode.OK,"Already logged out")

        }
        post("/register"){
            val registrationRequest = call.receive<RegistrationRequest>()
            val registrationResult = userRepository.registerNewUser(registrationRequest.toUserDto())
            registrationResult.fold(
                onSuccess = { user->
                    call.respond(HttpStatusCode.OK, user)
                },
                onFailure = {
                    call.respond(HttpStatusCode.Conflict, "User with this phone already registered")
                }
            )

        }
    }
}