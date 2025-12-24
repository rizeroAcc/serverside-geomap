package com.mapprjct.database.dao

interface SessionRepository {
    suspend fun upsert(id: String, value: String, phone: String)
    suspend fun delete(id: String)
    suspend fun get(id: String): String?
    suspend fun deleteAllUserSessions(phone: String)
}