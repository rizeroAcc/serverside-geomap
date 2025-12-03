package com.mapprjct.feature.authentication.authorization

import com.mapprjct.feature.authentication.registration.RegistrationRequest
import com.mapprjct.feature.authentication.registration.toUserDto
import com.mapprjct.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.configureAuthorizationRouting() {
    val userRepository: UserRepository by inject()
    routing{
        post("/signin") {
            val request = call.receive<SignInRequest>()
            if (userRepository.isAuthorizationValid(request.toUserDTO())){

            }else{
                call.respond(HttpStatusCode.Unauthorized, "Invalid authorization")
            }
        }
    }
}