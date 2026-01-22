package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object InviteCodeTable : Table("invite_code") {
    val inviteCode = uuid("invite_code").uniqueIndex()
    val projectID = reference("project_id", ProjectTable.id, onDelete = ReferenceOption.CASCADE)
    val inviterPhone = reference("inviter_phone",UserTable.phone, onDelete = ReferenceOption.CASCADE)
    val expireAt = long("expireAt")
    val role = short("role")

    override val primaryKey = PrimaryKey(inviteCode)
}