package com.mapprjct.app

import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.SessionTable
import com.mapprjct.database.tables.UserTable
import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDBTables(clearTables : Boolean = false) {
    transaction {
        if (clearTables) {
            SchemaUtils.drop(
                _root_ide_package_.com.mapprjct.database.tables.UserTable,
                _root_ide_package_.com.mapprjct.database.tables.SessionTable,
                _root_ide_package_.com.mapprjct.database.tables.ProjectTable,
                _root_ide_package_.com.mapprjct.database.tables.ProjectUsersTable,
                _root_ide_package_.com.mapprjct.database.tables.InviteCodeTable,
            )
        }
        SchemaUtils.create(
            _root_ide_package_.com.mapprjct.database.tables.UserTable,
            _root_ide_package_.com.mapprjct.database.tables.SessionTable,
            _root_ide_package_.com.mapprjct.database.tables.ProjectTable,
            _root_ide_package_.com.mapprjct.database.tables.ProjectUsersTable,
            _root_ide_package_.com.mapprjct.database.tables.InviteCodeTable,
        )
    }
}