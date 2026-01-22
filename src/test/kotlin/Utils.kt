package com.mapprjct

import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.koin.ktor.ext.getKoin

fun getTestResourceAsChannel(path : String) : ByteReadChannel {
    return ClassLoader.getSystemResourceAsStream(path)!!.toByteReadChannel()
}

inline fun <reified T : Any> ApplicationTestBuilder.getBean() : T {
    return this.application.getKoin().get()
}

fun buildMultipartFromFile(
    path : String,
    filename : String? = null,
    type : String? = null
) : MultiPartFormDataContent {
    val avatarData = getTestResourceAsChannel(path)
    val fileName = filename ?: path.substringAfterLast('/')
    val fileType = type ?: path.substringAfterLast('.')
    return MultiPartFormDataContent(
        parts = formData {
            this.append(
                key = "file",
                value = ChannelProvider{ avatarData },
                headers = Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=$fileName")
                    append(HttpHeaders.ContentType, "image/$fileType")
                }
            )
        }
    )
}