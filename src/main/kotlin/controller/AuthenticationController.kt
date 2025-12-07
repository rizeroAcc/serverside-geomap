package com.mapprjct.controller

import com.mapprjct.request.SignInRequest
import com.mapprjct.request.toUserCredentialsDTO
import com.mapprjct.dto.APISession
import com.mapprjct.response.SignInResponse
import com.mapprjct.request.RegistrationRequest
import com.mapprjct.request.toUserCredentialsDto
import com.mapprjct.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
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
            val user = userRepository.getUserCredentials(request.toUserCredentialsDTO())
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
            val registrationResult = userRepository.createUser(registrationRequest.toUserCredentialsDto())
            registrationResult.fold(
                onSuccess = { user->
                    call.respond(HttpStatusCode.OK, "Created")
                },
                onFailure = {
                    call.respond(
                        status = HttpStatusCode.Conflict,
                        message = "User with phone ${registrationRequest.phone} already registered"
                    )
                }
            )

        }
    }
}