package com.mapprjct

import com.mapprjct.database.projects.ProjectTable
import com.mapprjct.database.projects.ProjectUsersTable
import com.mapprjct.database.sessions.SessionTable
import com.mapprjct.database.users.UserTable
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDBTables(){
    transaction {
        SchemaUtils.create(
            UserTable,
            SessionTable,
            ProjectTable,
            ProjectUsersTable,
        )
    }
}