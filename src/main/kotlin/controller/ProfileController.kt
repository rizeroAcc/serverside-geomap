package com.mapprjct.controller

import com.mapprjct.database.storage.PostgresSessionStorage
import com.mapprjct.model.APISession
import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.service.UserService
import com.mapprjct.model.request.ChangePasswordRequest
import com.mapprjct.model.request.ChangeUserInfoRequest
import com.mapprjct.model.response.AvatarUpdateResponse
import com.mapprjct.model.response.ChangePasswordResponse
import com.mapprjct.model.response.error.ErrorResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.koin.ktor.ext.inject
import java.io.File

fun Application.configureProfileController() {
    val userService : UserService by inject()
    val sessionStorage : SessionStorage by inject()
    routing {
        authenticate("auth-session") {
            route("/user"){
                getUserInfo(userService)
                updateUserAvatar(userService)
                get("/avatar/{filename}") {
                    val filename = call.parameters["filename"] ?: return@get call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = "Missing filename"
                    )

                    // Безопасность: проверка имени файла
                    if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }

                    val file = File("api/uploads/avatars", filename)

                    if (!file.exists()) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }

                    // Устанавливаем заголовки кэширования
                    call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=31536000")

                    // Отдаем файл
                    call.respondFile(file)
                }
                post("/changePassword"){
                    val session = call.principal<APISession>()!!
                    //if session valid user never be null
                    val request = call.receive<ChangePasswordRequest>()
                    val oldCredentials = UserCredentials(
                        phone = session.phone,
                        password = request.oldPassword
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
                        call.respond(HttpStatusCode.BadRequest, error.message!!)
                    })
                }
                patch("/"){
                    val session = call.principal<APISession>()!!
                    val request = call.receive<ChangeUserInfoRequest>()
                    var newUserInfo = userService.getUser(session.phone).getOrElse{
                        call.respond(HttpStatusCode.InternalServerError, "Database unavailable")
                        return@patch
                    }
                    if (newUserInfo == null) {
                        call.respond(HttpStatusCode.NoContent, "User not found")
                        return@patch
                    }
                    if (request.username != null){
                        newUserInfo = newUserInfo.copy(username = request.username)
                    }
                    userService.updateUser(newUserInfo).fold(
                        onSuccess = { result->
                            call.respond(status = HttpStatusCode.Accepted, message = result!!)
                        },
                        onFailure = {
                            call.respond(HttpStatusCode.InternalServerError,it.message!!)
                        }
                    )
                }
            }
        }
    }
}

private fun Route.updateUserAvatar(userService: UserService){
    post("/avatar"){
        val session = call.principal<APISession>()!!
        val user = userService.getUser(session.phone).getOrElse { error->
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse.loggedDatabaseException(error as ExposedSQLException)
            )
            return@post
        }!!

        try {
            val multipart = call.receiveMultipart()
            userService.updateUserAvatar(user,multipart).fold(
                onSuccess = { user->
                    call.respond(
                        HttpStatusCode.Accepted,
                        AvatarUpdateResponse(
                            user = user
                        )
                    )
                },
                onFailure = {
                    call.respond(HttpStatusCode.BadRequest,it.message!!)
                }
            )
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid file")
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Upload failed: ${e.message}")
        }
    }
}

private fun Route.getUserInfo(userService: UserService){
    get(){
        val session = call.principal<APISession>()!!
        //not null if session valid
        val user = userService.getUser(session.phone).getOrElse { error->
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse.loggedDatabaseException(error as ExposedSQLException)
            )
            return@get
        }!!
        call.respond(status = HttpStatusCode.OK,user)
    }
}

