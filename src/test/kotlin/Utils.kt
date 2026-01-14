package com.mapprjct

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