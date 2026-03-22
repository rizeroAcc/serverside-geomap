package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object ProjectUsersTable : Table("project_users") {
    val projectId = reference("project_id",ProjectTable.id, onDelete = ReferenceOption.CASCADE)
    val userPhone = reference("user_phone",UserTable.phone, onDelete = ReferenceOption.CASCADE)
    val role = short("role")

    override val primaryKey = PrimaryKey(projectId,userPhone)
    init{
        index(customIndexName = "project_user", isUnique = true, projectId,userPhone)
    }
}