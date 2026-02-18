package com.mapprjct.controller

import com.mapprjct.controller.util.respondBadRequest
import com.mapprjct.controller.util.respondDatabaseError
import com.mapprjct.controller.util.respondElementNotFound
import com.mapprjct.controller.util.respondForbidden
import com.mapprjct.controller.util.respondRequestTimeout
import com.mapprjct.controller.util.respondServerError
import com.mapprjct.controller.util.respondUnexpected
import com.mapprjct.database.storage.impl.PostgresSessionStorage
import com.mapprjct.exceptions.domain.user.DeleteUserAvatarException
import com.mapprjct.exceptions.domain.user.FindUserAvatarException
import com.mapprjct.exceptions.domain.user.FindUserException
import com.mapprjct.exceptions.domain.user.UpdateAvatarException
import com.mapprjct.exceptions.domain.user.UpdateUserPasswordException
import com.mapprjct.exceptions.domain.user.UserUpdateException
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
import com.mapprjct.utils.fold
import com.mapprjct.utils.getOrElse
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
                                is UpdateAvatarException.ConnectionTerminated -> respondRequestTimeout()
                                is UpdateAvatarException.DatabaseError -> respondDatabaseError()
                                is UpdateAvatarException.FilesystemUnavailable -> respondServerError("Filesystem is unavailable. Try later")
                                is UpdateAvatarException.InvalidAvatarFormat -> respondBadRequest("Invalid avatar format. Allowed file format: ${error.allowedFormat}")
                                is UpdateAvatarException.Unexpected -> respondUnexpected()
                                is UpdateAvatarException.UserNotFound -> respondUnexpected()
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
        }.onFailure { exception->
            when (exception) {
                is ContentTransformationException -> respondBadRequest("Request have invalid multipart format")
                is IllegalArgumentException -> respondBadRequest(exception.message!!)
            }
        }
    }
}

private fun Route.getProfileAvatar(userService : UserService) {
    get("/avatar") {
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        userService.getUserAvatar(userPhone).fold(
            onSuccess = { avatar->
                call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=31536000")
                call.respondFile(avatar)
            },
            onError = { error->
                when (error) {
                    is FindUserAvatarException.DatabaseError -> respondDatabaseError()
                    is FindUserAvatarException.Unexpected -> respondUnexpected()
                    is FindUserAvatarException.UserAvatarNotFound -> respondElementNotFound("User avatar not found")
                    is FindUserAvatarException.UserNotFound -> respondUnexpected()
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
            onSuccess = { user->
                call.respond(HttpStatusCode.OK, DeleteAvatarResponse(user))
            },
            onError = { error ->
                when (error) {
                    is DeleteUserAvatarException.DatabaseError -> respondDatabaseError()
                    is DeleteUserAvatarException.FileSystemUnavailable -> respondServerError("Filesystem is unavailable. Try later")
                    is DeleteUserAvatarException.Unexpected -> respondUnexpected()
                    is DeleteUserAvatarException.UserAvatarNotFound -> respondElementNotFound("User avatar not found")
                    is DeleteUserAvatarException.UserNotFound -> respondUnexpected()
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
                call.respond(status = HttpStatusCode.Accepted, message = UpdateUserInfoResponse(updatedUser))
            },
            onError = { error ->
                when (error) {
                    is UserUpdateException.DatabaseError -> respondDatabaseError()
                    is UserUpdateException.Unexpected -> respondUnexpected()
                    is UserUpdateException.UserNotFound -> respondUnexpected()
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
            newUserPassword = Password(request.newPassword)
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
        }, onError = { error->
            when (error) {
                is UpdateUserPasswordException.DatabaseError -> respondDatabaseError()
                is UpdateUserPasswordException.IncorrectPassword -> respondForbidden("Wrong password")
                is UpdateUserPasswordException.Unexpected -> respondUnexpected()
                is UpdateUserPasswordException.UserNotFound -> respondUnexpected()
            }
        })
    }
}

private fun Route.getProfileInfo(userService: UserService){
    get(){
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        userService.getUser(userPhone).fold(
            onSuccess = { user->
                call.respond(status = HttpStatusCode.OK,user)
            }, onError = { error->
                when (error) {
                    is FindUserException.Database -> respondDatabaseError()
                    is FindUserException.Unexpected -> respondUnexpected()
                    is FindUserException.UserNotFound -> respondUnexpected()
                }
            }
        )


    }
}

