package com.mapprjct.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ProjectUsersTable : Table("project_users") {
    val projectId = uuid("project_id").references(ProjectTable.id, onUpdate = ReferenceOption.CASCADE)
    val userPhone = varchar("user_phone",12)
    val role = short("role")

    override val primaryKey = PrimaryKey(projectId,userPhone)
}