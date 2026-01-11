package com.mapprjct.di

import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.database.storage.impl.FileAvatarStorage
import com.mapprjct.database.storage.impl.PostgresSessionStorage
import io.ktor.server.sessions.SessionStorage
import org.koin.dsl.module

val storageModule = module {
    single<SessionStorage> { PostgresSessionStorage(get()) }
    single<AvatarStorage> { FileAvatarStorage(get()) }
}