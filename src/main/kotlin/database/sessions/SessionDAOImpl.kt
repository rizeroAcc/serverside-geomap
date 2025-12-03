package com.mapprjct.database.sessions

import com.mapprjct.database.dao.SessionDAO
import com.mapprjct.database.sessions.SessionTable.data
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert

class SessionDAOImpl(val database: Database) : SessionDAO {
    override suspend fun upsert(id: String, value: String) {
        transaction(database) {
            SessionTable.upsert(
                onUpdate = {
                    it[SessionTable.data] = value
                }
            ){
                it[SessionTable.id] = id
                it[SessionTable.data] = value
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
            it[data]
        }
    }

}