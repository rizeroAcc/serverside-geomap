package com.mapprjct.feature.authentication.registration

import com.mapprjct.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRegistrationRouting() {
    val userRepository: UserRepository by inject()
    routing{
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