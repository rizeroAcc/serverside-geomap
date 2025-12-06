package com.mapprjct.feature.authentication.sessions

import com.mapprjct.database.sessions.APISession
import com.mapprjct.database.dao.SessionDAO
import io.ktor.server.sessions.SessionStorage
import kotlinx.serialization.json.Json


class PostgresSessionStorage (val sessionDAO: SessionDAO) : SessionStorage {
    override suspend fun write(id: String, value: String) {
        val phone = Json.decodeFromString<APISession>(value).phone
        sessionDAO.deleteAllUserSessions(phone)
        sessionDAO.upsert(id = id, value = value, phone = phone)
    }

    override suspend fun invalidate(id: String) {
        sessionDAO.delete(id)
    }

    override suspend fun read(id: String): String {
        return sessionDAO.get(id)?: throw NoSuchElementException("Session with id $id not found")
    }
}