package com.mapprjct.controller

import com.mapprjct.database.storage.PostgresSessionStorage
import com.mapprjct.model.APISession
import com.mapprjct.dto.UserCredentials
import com.mapprjct.repository.UserService
import com.mapprjct.request.ChangePasswordRequest
import com.mapprjct.request.ChangeUserInfoRequest
import com.mapprjct.response.AvatarUpdateResponse
import com.mapprjct.response.ChangePasswordResponse
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
import org.koin.ktor.ext.inject
import java.io.File

fun Application.configureProfileController() {
    val userService : UserService by inject()
    val sessionStorage : SessionStorage by inject()
    routing {
        authenticate("auth-session") {
            route("/user"){
                get("/"){
                    val session = call.principal<APISession>()!!
                    //if session valid user never be null
                    val user = userService.getUser(session.phone)!!
                    call.respond(status = HttpStatusCode.OK,user)
                }
                post("/avatar"){
                    val session = call.principal<APISession>()!!
                    try {
                        val multipart = call.receiveMultipart()
                        var avatarFileName: String? = null
                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FileItem -> {
                                    val originalFileName = part.originalFileName as String
                                    val fileExtension = originalFileName.substringAfterLast('.').lowercase()
                                    if (!fileIsImage(originalFileName)) {
                                        throw IllegalArgumentException("Allowed formats: jpg, png")
                                    }

                                    // Генерация имени файла
                                    val fileName = "${session.phone}_avatar.$fileExtension"
                                    val uploadDir = getOrCreateUploadDirectory("api/uploads/avatars")

                                    val oldAvatarPath = userService.getUser(session.phone)?.avatarPath
                                    removeOldFile(uploadDir,oldAvatarPath)

                                    // Create new file
                                    val targetFile = File(uploadDir, fileName)
                                    //Write image
                                    part.provider().copyAndClose((targetFile.writeChannel()))

                                    // Обновляем в базе данных
                                    avatarFileName = fileName
                                    userService.updateUser(
                                        user = userService.getUser(session.phone)!!
                                            .copy(avatarPath = avatarFileName),
                                    )
                                }
                                else -> {}
                            }
                            part.dispose()
                        }

                        if (avatarFileName == null) {
                            call.respond(HttpStatusCode.BadRequest, "No file provided")
                            return@post
                        }

                        call.respond(
                            HttpStatusCode.Accepted,
                            AvatarUpdateResponse(
                                avatarUrl = "/api/avatars/$avatarFileName",
                            )
                        )

                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid file")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Upload failed: ${e.message}")
                    }
                }
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
                    var newUserInfo = userService.getUser(session.phone)!!
                    if (request.username != null){
                        newUserInfo = newUserInfo.copy(username = request.username)
                    }
                    userService.updateUser(newUserInfo).fold(
                        onSuccess = { result->
                            call.respond(HttpStatusCode.Accepted,result)
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

fun fileIsImage(filename : String) : Boolean {
    return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")
}
fun getOrCreateUploadDirectory(relatePath : String) : File {
    val directory = File(relatePath)
    if (!directory.exists()) {
        directory.mkdirs()
    }
    return directory
}

fun removeOldFile(directory : File, oldFilename : String?) {
    oldFilename?.let { oldFileName ->
        val oldFile = File(directory, oldFileName)
        if (oldFile.exists()) {
            oldFile.delete()
        }
    }
}