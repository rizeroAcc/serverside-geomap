package com.mapprjct.feature.authentication.sessions

import com.mapprjct.database.dao.SessionDAO
import com.mapprjct.database.sessions.SessionTable
import com.mapprjct.database.sessions.SessionTable.data
import io.ktor.server.sessions.SessionStorage
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


class PostgresSessionStorage (val sessionDAO: SessionDAO) : SessionStorage {
    override suspend fun write(id: String, value: String) {
        sessionDAO.upsert(id = id, value = value)
    }

    override suspend fun invalidate(id: String) {
        sessionDAO.delete(id)
    }

    override suspend fun read(id: String): String {
        return sessionDAO.get(id)?: throw NoSuchElementException("Session with id $id not found")
    }
}