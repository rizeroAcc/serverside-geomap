package com.mapprjct.database.projects

import org.jetbrains.exposed.sql.Table

object ProjectUsersTable : Table("project_users") {
    val projectId = uuid("project_id")
    val userPhone = varchar("user_phone",12)
    val role = ProjectUsersTable.short("role")

    override val primaryKey = PrimaryKey(projectId,userPhone)
}