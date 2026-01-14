package com.mapprjct.controller

import com.mapprjct.model.response.error.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

suspend fun RoutingContext.logDatabaseErrorAndRespondISE(e : ExposedSQLException) {
    //todo log exception here
    call.respond(InternalServerError, ErrorResponse.loggedDatabaseException(e))
}

suspend fun RoutingContext.respondBadRequest(reason : String) {
    call.respond(HttpStatusCode.BadRequest, ErrorResponse.fromText(reason))
}

suspend fun RoutingContext.logErrorAndRespondISE(e : Throwable, msg : String) {
    call.respond(InternalServerError, )
}

