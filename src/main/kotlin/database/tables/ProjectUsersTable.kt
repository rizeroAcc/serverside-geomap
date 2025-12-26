package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.Table

object ProjectUsersTable : Table("project_users") {
    val projectId = uuid("project_id").references(ProjectTable.id)
    val userPhone = varchar("user_phone",12).references(UserTable.phone)
    val role = short("role")

    override val primaryKey = PrimaryKey(projectId,userPhone)
    val index = index(customIndexName = "project_user", isUnique = true, projectId,userPhone)
}