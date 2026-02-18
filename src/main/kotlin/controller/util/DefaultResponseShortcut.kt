package com.mapprjct.controller.util

import com.mapprjct.model.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.RequestTimeout
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

suspend inline fun RoutingContext.respondDatabaseError(){
    call.respond(ServiceUnavailable, ErrorResponse.fromText("Server error. Database unavailable."))
}

suspend inline fun RoutingContext.respondUnexpected(message: String? = null){
    call.respond(InternalServerError, ErrorResponse.fromText(message = message?:"Unexpected error. Contact with administrators."))
}

suspend inline fun RoutingContext.respondConflict(message : String){
    call.respond(Conflict, ErrorResponse.fromText(message))
}

suspend inline fun RoutingContext.respondElementNotFound(message: String){
    call.respond(NotFound, ErrorResponse.fromText(message))
}

suspend inline fun RoutingContext.respondRequestTimeout(){
    call.respond(RequestTimeout, ErrorResponse.fromText("Request timeout"))
}

suspend inline fun RoutingContext.respondServerError(message: String){
    call.respond(InternalServerError, ErrorResponse.fromText(message))
}

suspend inline fun RoutingContext.respondForbidden(message: String? = null){}

suspend inline fun RoutingContext.respondBadRequest(message : String) {
    call.respond(HttpStatusCode.BadRequest, ErrorResponse.fromText(message))
}



