@file:OptIn(ExperimentalTime::class)

package com.mapprjct.controller

import arrow.core.getOrElse
import com.mapprjct.controller.util.respondConflict
import com.mapprjct.controller.util.respondDatabaseError
import com.mapprjct.controller.util.respondUnexpected
import com.mapprjct.database.storage.impl.PostgresSessionStorage
import com.mapprjct.exceptions.domain.user.ValidateCredentialError
import com.mapprjct.exceptions.domain.user.FindUserException
import com.mapprjct.exceptions.domain.user.CreateUserError
import com.mapprjct.model.request.auth.SignInRequest
import com.mapprjct.model.request.auth.toUserCredentialsDTO
import com.mapprjct.model.APISession
import com.mapprjct.model.response.auth.SignInResponse
import com.mapprjct.model.request.auth.RegistrationRequest
import com.mapprjct.model.request.auth.toUserCredentialsDto
import com.mapprjct.model.response.auth.RegistrationResponse
import com.mapprjct.model.response.profile.ChangePasswordResponse
import com.mapprjct.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import org.koin.ktor.ext.inject
import kotlin.getValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun Application.configureAuthenticationController() {
    val userService: UserService by inject()
    val sessionStorage : SessionStorage by inject()
    routing{
        authenticate("auth-session") {
            get("/validate_token"){
                call.respond(HttpStatusCode.OK)
            }
            post("/refresh_token"){
                val session = call.principal<APISession>()!!
                val oldSessionId = call.request.headers["Authorization"]!!
                call.sessions.clear<APISession>()
                sessionStorage.invalidate(oldSessionId)
                val newSession = APISession(
                    phone = session.phone ,
                    expireAt = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 168 //7 days
                )
                //Костыль, но иначе ktor переиспользует SessionID
                val newSessionID = (sessionStorage as PostgresSessionStorage).writeSession(newSession)
                call.response.headers.append("Authorization", newSessionID)
                call.respond(
                    status = HttpStatusCode.OK,
                    message = ChangePasswordResponse(newSession.expireAt)
                )
            }
        }
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
            userCredentialsDTO = request.toUserCredentialsDTO()
        ).getOrElse { error->
            when (error) {
                is ValidateCredentialError.Database -> respondDatabaseError()
                is ValidateCredentialError.Unexpected -> respondUnexpected()
            }
            return@post
        }
        if (!credentialsValid){
            call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
        }
        val user = userService.getUser(request.phone).getOrElse { error->
            when (error) {
                is FindUserException.Database -> respondDatabaseError()
                is FindUserException.Unexpected -> respondUnexpected()
                is FindUserException.UserNotFound -> respondUnexpected() //if credentials valid - user must exists
            }
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
                userDTO = user,
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
            userCredentialsDTO = userCredentials,
            username = username
        )
        registrationResult.fold(
            ifRight = { user->
                call.respond(HttpStatusCode.Created,
                    RegistrationResponse(user)
                )
            },
            ifLeft = { error->
                when(error){
                    is CreateUserError.Database -> respondDatabaseError()
                    is CreateUserError.Unexpected -> respondUnexpected()
                    is CreateUserError.UserAlreadyExists -> respondConflict("User with phone: ${error.phone} already registered")
                }
            }
        )
    }
}
