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
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.koin.ktor.ext.inject
import java.io.File

fun Application.configureProfileController() {
    val userRepository : UserRepository by inject()
    routing {
        route("/user"){
            get("/"){
                val session = call.sessions.get<APISession>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "Not authenticated")
                    return@get
                }
                val user = userRepository.getUser(session.phone)
                if (user == null){
                    call.respond(status = HttpStatusCode.BadRequest,message ="Invalid token")
                }else{
                    call.respond(status = HttpStatusCode.OK,user)
                }
            }
            post("/avatar"){

                val session = call.sessions.get<APISession>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "Not authenticated")
                    return@post
                }

                try {
                    val multipart = call.receiveMultipart()
                    var avatarFileName: String? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val allowedExtensions = setOf("jpg", "jpeg", "png")
                                val originalFileName = part.originalFileName ?: "avatar"
                                val fileExtension = originalFileName.substringAfterLast('.').lowercase()

                                if (!allowedExtensions.contains(fileExtension)) {
                                    throw IllegalArgumentException("Allowed formats: jpg, png")
                                }

                                // Генерация имени файла
                                val fileName = "${session.phone}_avatar.$fileExtension"
                                val uploadDir = File("api/uploads/avatars")

                                if (!uploadDir.exists()) {
                                    uploadDir.mkdirs()
                                }

                                val oldAvatarPath = userRepository.getUser(session.phone)?.avatar
                                oldAvatarPath?.let { oldFileName ->
                                    val oldFile = File(uploadDir, oldFileName)
                                    if (oldFile.exists()) {
                                        oldFile.delete()
                                    }
                                }

                                // Сохраняем новый файл
                                val targetFile = File(uploadDir, fileName)
                                part.streamProvider().use { input ->
                                    targetFile.outputStream().buffered().use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                // Обновляем в базе данных
                                avatarFileName = fileName
                                userRepository.updateUser(user = User(
                                    phone = session.phone,
                                    avatar = avatarFileName
                                ))
                                part.dispose()
                            }

                            else -> part.dispose()
                        }
                    }

                    if (avatarFileName == null) {
                        call.respond(HttpStatusCode.BadRequest, "No file provided")
                        return@post
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        AvatarUpdateResponse(
                            success = true,
                            avatarUrl = "/api/avatars/$avatarFileName",
                            message = "Avatar uploaded successfully"
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