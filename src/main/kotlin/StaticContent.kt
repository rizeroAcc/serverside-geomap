package com.mapprjct

import com.mapprjct.dto.APISession
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.default
import io.ktor.server.http.content.files
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticBasePackage
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import java.io.File

@Deprecated("move to api")
fun Application.configureStaticContent() {
    routing {
        get("/api/avatars/{filename}") {
            val session = call.sessions.get<APISession>()?:  run {
                call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
            }
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

            // Определяем Content-Type
            val contentType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> ContentType.Image.JPEG
                "png" -> ContentType.Image.PNG
                else -> ContentType.Image.Any
            }

            // Устанавливаем заголовки кэширования
            call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=31536000")

            // Отдаем файл
            call.respondFile(file)
        }
    }
}