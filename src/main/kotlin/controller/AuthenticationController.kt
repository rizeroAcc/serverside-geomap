@file:OptIn(ExperimentalTime::class)

package com.mapprjct.controller

import com.mapprjct.database.storage.impl.PostgresSessionStorage
import com.mapprjct.model.request.auth.SignInRequest
import com.mapprjct.model.request.auth.toUserCredentialsDTO
import com.mapprjct.model.APISession
import com.mapprjct.model.ErrorResponse
import com.mapprjct.model.response.auth.SignInResponse
import com.mapprjct.model.request.auth.RegistrationRequest
import com.mapprjct.model.request.auth.toUserCredentialsDto
import com.mapprjct.model.response.auth.RegistrationResponse
import com.mapprjct.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.sessions
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.koin.ktor.ext.inject
import kotlin.getValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun Application.configureAuthenticationController() {
    val userService: UserService by inject()
    val sessionStorage : SessionStorage by inject()
    routing{
        signInRoute(userService = userService, sessionStorage = sessionStorage)
        logOutRoute(sessionStorage)
        registrationRoute(userService)
    }
}

private fun Routing.signInRoute(
    userService: UserService,
    sessionStorage : SessionStorage,
) {
    post("/signin") {
        val request = call.receive<SignInRequest>()
        val credentialsValid = userService.validateCredentials(
            request.toUserCredentialsDTO()
        ).getOrElse { error->
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse.loggedDatabaseException(error as ExposedSQLException)
            )
            return@post
        }
        if (!credentialsValid){
            call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
        }
        val user = userService.getUser(request.phone.value).getOrElse { error->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse.loggedDatabaseException(error as ExposedSQLException))
            return@post
        }
        (sessionStorage as PostgresSessionStorage).clearUserSessions(user.phone.value)
        val session = APISession(
            phone = user.phone.value ,
            expireAt = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 168 //7 days
        )
        call.sessions.set("Authorization" , session)
        call.respond(
            status = HttpStatusCode.OK,
            message = SignInResponse(
                user = user,
                tokenExpiration = session.expireAt
            )
        )
    }
}

private fun Routing.logOutRoute(sessionStorage : SessionStorage) {
    post("/logout") {
        val sessionId = call.request.headers["Authorization"]
        sessionId?.let {
            try {
                sessionStorage.read(it)
            }catch (_ : NoSuchElementException){
                call.respond(HttpStatusCode.NotFound,"Invalid session")
                return@post
            }
            sessionStorage.invalidate(it)
            call.respond(HttpStatusCode.Accepted,"Logged out")
            return@post
        }
        call.respond(HttpStatusCode.NoContent,"No token found")
    }
}

private fun Routing.registrationRoute(userService : UserService){
    post("/register"){
        val registrationRequest = call.receive<RegistrationRequest>()
        val userCredentials = registrationRequest.toUserCredentialsDto()
        val username = registrationRequest.username
        val registrationResult = userService.createUser(
            userCredentials = userCredentials,
            username = username.value
        )
        registrationResult.fold(
            onSuccess = { user->
                call.respond(HttpStatusCode.Created,
                    RegistrationResponse(user)
                )
            },
            onFailure = { error->
                when(error){
                    is UserValidationException-> call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = ErrorResponse.fromAppException(error)
                    )
                    is UserDMLExceptions.UserAlreadyExistsException -> call.respond(
                        status = HttpStatusCode.Conflict,
                        message = ErrorResponse.fromAppException(error)
                    )
                    is ExposedSQLException -> call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = ErrorResponse.loggedDatabaseException(error)
                    )
                }
            }
        )
    }
}
