package com.mapprjct.database.dao

import com.mapprjct.database.sessions.SessionTable
import org.h2.engine.Session
import org.jetbrains.exposed.sql.transactions.transaction

interface SessionDAO {
    suspend fun upsert(id: String, value: String)
    suspend fun delete(id: String)
    suspend fun get(id: String): String?
}