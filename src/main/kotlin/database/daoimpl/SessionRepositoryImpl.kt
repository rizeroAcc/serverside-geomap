package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.SessionRepository
import com.mapprjct.database.tables.SessionTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert

class SessionRepositoryImpl(val database: Database) : SessionRepository {
    override suspend fun upsert(id: String, value: String, phone: String) {
        transaction(database) {
            SessionTable.upsert(
                onUpdate = {
                    it[SessionTable.data] = value
                }
            ) {
                it[SessionTable.id] = id
                it[SessionTable.data] = value
                it[SessionTable.phone] = phone
            }
        }
    }

    override suspend fun delete(id: String) {
        transaction(database) {
            SessionTable.deleteWhere { SessionTable.id eq id }
        }
    }

    override suspend fun get(id: String): String? {
        val row = transaction(database) {
            SessionTable.selectAll().where { SessionTable.id eq id }
                .singleOrNull()
        }
        return row?.let {
            it[SessionTable.data]
        }
    }

    override suspend fun deleteAllUserSessions(phone: String) {
        transaction(database) {
            SessionTable.deleteWhere { SessionTable.phone eq phone }
        }
    }

}