package com.mapprjct.controller

import com.mapprjct.dto.APISession
import com.mapprjct.dto.User
import com.mapprjct.repository.UserRepository
import com.mapprjct.response.AvatarUpdateResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import org.koin.ktor.ext.inject
import java.io.File

fun Application.configureProfileController() {
    val userRepository : UserRepository by inject()
    routing {
        authenticate("auth-session") {
            route("/user"){
                get("/"){
                    val session = call.principal<APISession>()!!
                    //if session valid user never be null
                    val user = userRepository.getUser(session.phone)!!
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

                                    val oldAvatarPath = userRepository.getUser(session.phone)?.avatar
                                    removeOldFile(uploadDir,oldAvatarPath)

                                    // Create new file
                                    val targetFile = File(uploadDir, fileName)
                                    //Write image
                                    part.provider().copyAndClose((targetFile.writeChannel()))

                                    // Обновляем в базе данных
                                    avatarFileName = fileName
                                    userRepository.updateUser(user = User(
                                        phone = session.phone,
                                        username = "",
                                        avatar = avatarFileName
                                    ))
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