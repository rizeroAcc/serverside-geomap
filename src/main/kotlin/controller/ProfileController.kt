package com.mapprjct.controller

import com.mapprjct.database.storage.impl.PostgresSessionStorage
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.exceptions.user.UserValidationException
import com.mapprjct.model.APISession
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.service.UserService
import com.mapprjct.model.request.profile.ChangePasswordRequest
import com.mapprjct.model.request.profile.ChangeUserInfoRequest
import com.mapprjct.model.response.profile.AvatarUpdateResponse
import com.mapprjct.model.response.profile.ChangePasswordResponse
import com.mapprjct.model.ErrorResponse
import com.mapprjct.model.response.profile.DeleteAvatarResponse
import com.mapprjct.model.response.profile.UpdateUserInfoResponse
import com.mapprjct.model.value.Password
import com.mapprjct.model.value.RussiaPhoneNumber
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.koin.ktor.ext.inject
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun Application.configureProfileController() {
    val userService : UserService by inject()
    val sessionStorage : SessionStorage by inject()
    routing {
        authenticate("auth-session") {
            route("/user"){
                updateProfileAvatar(userService)
                getProfileAvatar(userService)
                deleteProfileAvatar(userService)
                getProfileInfo(userService)
                changePassword(userService,sessionStorage)
                updateUserInfo(userService)
            }
        }
    }
}

private fun Route.updateProfileAvatar(userService: UserService){
    post("/avatar"){
        val session = call.principal<APISession>()!!
        val user = userService.getUser(session.phone).getOrElse { error->
            logDatabaseErrorAndRespondISE(error as ExposedSQLException)
            return@post
        }

        runCatching {
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val receivedFileName = part.originalFileName
                        if(receivedFileName.isNullOrBlank()){
                            throw IllegalArgumentException("Empty file name")
                        }


                        val updatedUser = userService.updateUserAvatar(user,receivedFileName, part.provider).getOrThrow()
                        call.respond(
                            HttpStatusCode.Accepted,
                            AvatarUpdateResponse(updatedUser)
                        )
                    }
                    else -> {}
                }
                part.dispose()
            }
        }.onFailure { exception->
            when (exception) {
                is ContentTransformationException -> respondBadRequest("Request have invalid multipart format")
                is IllegalArgumentException -> respondBadRequest(exception.message!!)
                is IOException -> {
                    call.respond(
                        status = HttpStatusCode.ServiceUnavailable,
                        message = ErrorResponse.fromText("Server file system busy try later")
                    )
                }
                else ->logErrorAndRespondISE(exception, "Unexpected error, see logs for details")
            }
        }
    }
}

private fun Route.getProfileAvatar(userService : UserService) {
    get("/avatar") {
        val session = call.principal<APISession>()!!
        val user = userService.getUser(session.phone).getOrElse {
            logErrorAndRespondISE(it, "User not found")
            return@get
        }
        userService.getUserAvatar(user.phone.value).fold(
            onSuccess = { avatar->
                call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=31536000")
                call.respondFile(avatar)
            },
            onFailure = { exception->
                when (exception) {
                    is FileNotFoundException -> call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse.fromText("Avatar corrupted. Please contact with administrators to recover it, or reupload it")
                    )
                    is UserDMLExceptions.UserAvatarNotFoundException -> call.respond(
                        status = HttpStatusCode.NoContent,
                        message = ErrorResponse.fromText("User hasn't avatar")
                    )
                }
            }
        )

    }
}

private fun Route.deleteProfileAvatar(userService: UserService) {
    delete("/avatar") {
        val session = call.principal<APISession>()!!
        val user = userService.getUser(session.phone).getOrElse {
            logErrorAndRespondISE(it, "User not found")
            return@delete
        }
        userService.deleteUserAvatar(user).fold(
            onSuccess = {
                call.respond(HttpStatusCode.OK, DeleteAvatarResponse(user.copy(avatarFilename = null)))
            },
            onFailure = { exception ->
                when (exception) {
                    is UserDMLExceptions.UserAvatarNotFoundException -> call.respond(HttpStatusCode.NotFound)
                    else -> logErrorAndRespondISE(exception, "Unexpected error")
                }
            }
        )
    }
}

private fun Route.updateUserInfo(userService: UserService) {
    patch(""){
        val newUserInfo = call.receive<ChangeUserInfoRequest>().user
        userService.updateUser(newUserInfo).fold(
            onSuccess = { updatedUser->
                call.respond(status = HttpStatusCode.Accepted, message = UpdateUserInfoResponse(updatedUser!!))
            },
            onFailure = { exception ->
                when(exception){
                    is UserDMLExceptions.UserNotFoundException -> call.respond(
                        HttpStatusCode.NotFound, ErrorResponse.fromText("Incorrect user phone")
                    )
                    is UserValidationException -> respondBadRequest(exception.shortMessage)
                    is ExposedSQLException -> logDatabaseErrorAndRespondISE(exception)
                }
            }
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun Route.changePassword(userService: UserService, sessionStorage: SessionStorage) {
    post("/changePassword"){
        val session = call.principal<APISession>()!!
        val request = call.receive<ChangePasswordRequest>()
        val oldCredentials = UserCredentials(
            phone = RussiaPhoneNumber(session.phone),
            password = Password(request.oldPassword)
        )

        userService.updateUserPassword(
            oldCredentials = oldCredentials,
            newUserPassword = request.newPassword
        ).fold(onSuccess = {
            val sessionId = call.request.headers["Authorization"]
            call.sessions.clear<APISession>()
            sessionStorage.invalidate(sessionId!!)
            val newSession = APISession(
                phone = session.phone ,
                expireAt = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 168 //7 days
            )
            //Костыль, но иначе ktor переиспользует SessionID
            val newSessionID = (sessionStorage as PostgresSessionStorage).writeSession(newSession)
            call.response.headers.append("Authorization", newSessionID)
            call.respond(
                status = HttpStatusCode.Accepted,
                message = ChangePasswordResponse(newSession.expireAt)
            )
        }, onFailure = { error->
            when (error) {
                is UserDMLExceptions.UserNotFoundException -> logErrorAndRespondISE(error, "User not found. See logs")
                is IllegalArgumentException -> respondBadRequest("Incorrect old password")
                is ExposedSQLException -> logDatabaseErrorAndRespondISE(error)
                else -> logErrorAndRespondISE(error,"Unexpected error")
            }
        })
    }
}

private fun Route.getProfileInfo(userService: UserService){
    get(){
        val session = call.principal<APISession>()!!
        val user = userService.getUser(session.phone).getOrElse { error->
            when(error){
                is UserDMLExceptions.UserNotFoundException -> logErrorAndRespondISE(IllegalStateException(), "User not found. See logs")
                else -> logDatabaseErrorAndRespondISE(error as ExposedSQLException)
            }
            return@get
        }
        call.respond(status = HttpStatusCode.OK,user)
    }
}

