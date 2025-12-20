package com.mapprjct.database.storage

import com.mapprjct.database.dao.SessionDAO
import com.mapprjct.model.APISession
import io.ktor.server.sessions.SessionStorage
import kotlinx.serialization.json.Json
import java.util.UUID

class PostgresSessionStorage (val sessionDAO: SessionDAO) : SessionStorage {
    override suspend fun write(id: String, value: String) {
        val phone = Json.Default.decodeFromString<APISession>(value).phone
        sessionDAO.upsert(id = id, value = value, phone = phone)
    }

    override suspend fun invalidate(id: String) {
        sessionDAO.delete(id)
    }

    override suspend fun read(id: String): String {
        return sessionDAO.get(id)?: throw NoSuchElementException("Session with id $id not found")
    }

    suspend fun clearUserSessions(phone: String) {
        sessionDAO.deleteAllUserSessions(phone)
    }
    suspend fun writeSession(session: APISession) : String {
        val newSessionID = UUID.randomUUID().toString().replace("-", "")
        val serializedSession = Json.Default.encodeToString(session)
        write(newSessionID,serializedSession)
        return newSessionID
    }
}