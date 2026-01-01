package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.Table

object InviteCodeTable : Table("invite_code") {
    val inviteCode = uuid("invite_code").uniqueIndex()
    val projectID = reference("project_id", ProjectTable.id)
    val inviterPhone = reference("inviter_phone",UserTable.phone)
    val expireAt = long("expireAt")
    val role = short("role")

    override val primaryKey = PrimaryKey(inviteCode)
}