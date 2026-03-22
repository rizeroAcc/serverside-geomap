package com.mapprjct.controller

import arrow.core.getOrElse
import com.mapprjct.controller.util.respondBadRequest
import com.mapprjct.controller.util.respondDatabaseError
import com.mapprjct.controller.util.respondElementNotFound
import com.mapprjct.controller.util.respondForbidden
import com.mapprjct.controller.util.respondNoContent
import com.mapprjct.controller.util.respondRequestTimeout
import com.mapprjct.controller.util.respondServerError
import com.mapprjct.controller.util.respondServiceUnavailable
import com.mapprjct.controller.util.respondUnexpected
import com.mapprjct.database.storage.impl.PostgresSessionStorage
import com.mapprjct.exceptions.domain.user.DeleteUserAvatarError
import com.mapprjct.exceptions.domain.user.FindUserAvatarError
import com.mapprjct.exceptions.domain.user.FindUserException
import com.mapprjct.exceptions.domain.user.UpdateAvatarError
import com.mapprjct.exceptions.domain.user.UpdateUserPasswordError
import com.mapprjct.exceptions.domain.user.UpdateUserError
import com.mapprjct.model.APISession
import com.mapprjct.model.dto.UserCredentialsDTO
import com.mapprjct.service.UserService
import com.mapprjct.model.request.profile.ChangePasswordRequest
import com.mapprjct.model.request.profile.ChangeUserInfoRequest
import com.mapprjct.model.response.profile.AvatarUpdateResponse
import com.mapprjct.model.response.profile.ChangePasswordResponse
import com.mapprjct.model.response.profile.DeleteAvatarResponse
import com.mapprjct.model.response.profile.UpdateUserInfoResponse
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
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
import io.ktor.utils.io.CancellationException
import org.koin.ktor.ext.inject
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
        val userPhone = RussiaPhoneNumber(session.phone)
        val user = userService.getUser(userPhone).getOrElse { error->
            when(error){
                is FindUserException.Database -> respondDatabaseError()
                is FindUserException.Unexpected -> respondUnexpected()
                is FindUserException.UserNotFound -> respondUnexpected()
            }
            return@post
        }

        runCatching {
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val receivedFileName = part.originalFileName
                        if(receivedFileName.isNullOrBlank()) throw IllegalArgumentException("Empty file name")

                        val updatedUser = userService.updateUserAvatar(user,receivedFileName, part.provider).getOrElse { error->
                            when(error){
                                is UpdateAvatarError.ConnectionTerminated -> respondRequestTimeout()
                                is UpdateAvatarError.Database -> respondDatabaseError()
                                is UpdateAvatarError.FilesystemUnavailable -> respondServiceUnavailable("Filesystem is unavailable. Try later")
                                is UpdateAvatarError.InvalidAvatarFormat -> respondBadRequest("Invalid avatar format. Allowed file format: ${error.allowedFormat}")
                                is UpdateAvatarError.Unexpected -> respondUnexpected()
                                is UpdateAvatarError.UserNotFound -> respondUnexpected()
                            }
                            return@forEachPart
                        }
                        call.respond(
                            HttpStatusCode.Accepted,
                            AvatarUpdateResponse(updatedUser)
                        )
                    }
                    else -> {}
                }
                part.dispose()
            }
            println("\n\n\n ---- exit for each part ---- \n\n\n")
            return@post
        }.onFailure { exception->
            when (exception) {
                is ContentTransformationException -> respondBadRequest("Request have invalid multipart format")
                is IllegalArgumentException -> respondBadRequest(exception.message!!)
                is CancellationException -> throw exception
                else -> respondUnexpected()
            }
        }
    }
}

private fun Route.getProfileAvatar(userService : UserService) {
    get("/avatar") {
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        userService.getUserAvatar(userPhone).fold(
            ifRight = { avatar->
                call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=31536000")
                call.respondFile(avatar)
            },
            ifLeft = { error->
                when (error) {
                    is FindUserAvatarError.Database -> respondDatabaseError()
                    is FindUserAvatarError.Unexpected -> respondUnexpected()
                    is FindUserAvatarError.UserAvatarNotFound -> respondNoContent("User avatar not found")
                    is FindUserAvatarError.UserNotFound -> respondUnexpected()
                }
            }
        )
    }
}

private fun Route.deleteProfileAvatar(userService: UserService) {
    delete("/avatar") {
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        userService.deleteUserAvatar(userPhone).fold(
            ifRight = { user->
                call.respond(HttpStatusCode.OK, DeleteAvatarResponse(user))
            },
            ifLeft = { error ->
                when (error) {
                    is DeleteUserAvatarError.Database -> respondDatabaseError()
                    is DeleteUserAvatarError.FileSystemUnavailable -> respondServerError("Filesystem is unavailable. Try later")
                    is DeleteUserAvatarError.Unexpected -> respondUnexpected()
                    is DeleteUserAvatarError.UserAvatarNotFound -> respondElementNotFound("User avatar not found")
                    is DeleteUserAvatarError.UserNotFound -> respondUnexpected()
                }
            }
        )
    }
}

private fun Route.updateUserInfo(userService: UserService) {
    patch(""){
        val newUserInfo = call.receive<ChangeUserInfoRequest>().userDTO
        userService.updateUser(newUserInfo).fold(
            ifRight = { updatedUser->
                call.respond(status = HttpStatusCode.Accepted, message = UpdateUserInfoResponse(updatedUser))
            },
            ifLeft = { error ->
                when (error) {
                    is UpdateUserError.Database -> respondDatabaseError()
                    is UpdateUserError.Unexpected -> respondUnexpected()
                    is UpdateUserError.UserNotFound -> respondBadRequest("Try to change user phone")
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
        val oldCredentials = UserCredentialsDTO(
            phone = RussiaPhoneNumber(session.phone),
            password = Password(request.oldPassword)
        )

        userService.updateUserPassword(
            oldCredentials = oldCredentials,
            newUserPassword = Password(request.newPassword)
        ).fold(
            ifRight = {
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
        }, ifLeft = { error->
            when (error) {
                is UpdateUserPasswordError.Database -> respondDatabaseError()
                is UpdateUserPasswordError.IncorrectPassword -> respondForbidden("Wrong password")
                is UpdateUserPasswordError.Unexpected -> respondUnexpected()
                is UpdateUserPasswordError.UserNotFound -> respondUnexpected()
            }
        })
    }
}

private fun Route.getProfileInfo(userService: UserService){
    get(){
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        userService.getUser(userPhone).fold(
            ifRight = { user->
                call.respond(status = HttpStatusCode.OK,user)
            },
            ifLeft = { error->
                when (error) {
                    is FindUserException.Database -> respondDatabaseError()
                    is FindUserException.Unexpected -> respondUnexpected()
                    is FindUserException.UserNotFound -> respondUnexpected()
                }
            }
        )


    }
}

