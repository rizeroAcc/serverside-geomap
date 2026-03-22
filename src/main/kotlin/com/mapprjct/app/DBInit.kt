package com.mapprjct.app

import com.mapprjct.database.tables.UserTable
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Suppress("UnusedReceiverParameter")
fun Application.configureDBTables(clearTables : Boolean = false) {
    transaction {
        if (clearTables) {
            SchemaUtils.drop(
                UserTable,
                com.mapprjct.database.tables.SessionTable,
                com.mapprjct.database.tables.ProjectTable,
                com.mapprjct.database.tables.ProjectUsersTable,
                com.mapprjct.database.tables.InviteCodeTable,
            )
        }
        SchemaUtils.create(
            UserTable,
            com.mapprjct.database.tables.SessionTable,
            com.mapprjct.database.tables.ProjectTable,
            com.mapprjct.database.tables.ProjectUsersTable,
            com.mapprjct.database.tables.InviteCodeTable,
        )
    }
}