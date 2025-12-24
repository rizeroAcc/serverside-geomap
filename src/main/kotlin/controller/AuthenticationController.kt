package com.mapprjct.controller

import com.mapprjct.database.storage.PostgresSessionStorage
import com.mapprjct.request.SignInRequest
import com.mapprjct.request.toUserCredentialsDTO
import com.mapprjct.model.APISession
import com.mapprjct.response.SignInResponse
import com.mapprjct.request.RegistrationRequest
import com.mapprjct.request.toUserCredentialsDto
import com.mapprjct.repository.UserService
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

fun Application.configureAuthenticationController() {
    val userService: UserService by inject()
    val sessionStorage : SessionStorage by inject()
    routing{
        post("/signin") {
            val request = call.receive<SignInRequest>()
            if (!userService.validateCredentials(request.toUserCredentialsDTO())){
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
            //User never be null if credentials correct
            val user = userService.getUser(request.phone)!!
            (sessionStorage as PostgresSessionStorage).clearUserSessions(user.phone)
            val session = APISession(
                phone = user.phone ,
                expireAt = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 168 //7 days
            )
            call.sessions.set("Authorization" , session)
            call.respond(
                status = HttpStatusCode.OK,
                message = SignInResponse(
                    user = user!!,
                    tokenExpiration = session.expireAt
                )
            )
        }
        post("/logout") {
            val sessionId = call.request.headers["Authorization"]
            if (sessionId != null) {
                try {
                    sessionStorage.read(sessionId)
                }catch (e : Exception){
                    call.respond(HttpStatusCode.NotFound,"Invalid session")
                }
                sessionStorage.invalidate(sessionId)
                call.respond(HttpStatusCode.Accepted,"Logged out")
            }
            call.respond(HttpStatusCode.OK,"Already logged out")
        }
        post("/register"){
            val registrationRequest = call.receive<RegistrationRequest>()
            val registrationResult = userService.createUser(
                userCredentials = registrationRequest.toUserCredentialsDto(),
                username = registrationRequest.username
            )
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