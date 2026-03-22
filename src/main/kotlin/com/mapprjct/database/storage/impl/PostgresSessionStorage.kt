package com.mapprjct.database.storage.impl

import com.mapprjct.database.repository.SessionRepository
import com.mapprjct.model.APISession
import io.ktor.server.sessions.SessionStorage
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID

class PostgresSessionStorage (val sessionRepository: SessionRepository) : SessionStorage {
    override suspend fun write(id: String, value: String) {
        val phone = Json.decodeFromString<APISession>(value).phone
        sessionRepository.upsert(id = id, value = value, phone = phone)
    }

    override suspend fun invalidate(id: String) {
        sessionRepository.delete(id)
    }

    override suspend fun read(id: String): String {
        return sessionRepository.get(id)?: throw NoSuchElementException("Session with id $id not found")
    }

    suspend fun clearUserSessions(phone: String) {
        suspendTransaction {
            sessionRepository.deleteAllUserSessions(phone)
        }
    }
    suspend fun writeSession(session: APISession) : String {
        val newSessionID = UUID.randomUUID().toString().replace("-", "")
        val serializedSession = Json.encodeToString(session)
        write(newSessionID,serializedSession)
        return newSessionID
    }
}