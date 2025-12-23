package com.mapprjct.database.tables

import org.jetbrains.exposed.sql.Table

object InviteCodeTable : Table("invite_code") {
    val inviteCode = uuid("invite_code").uniqueIndex()
    val projectID = uuid("project_id")
    val inviterPhone = varchar("inviter_phone", 12).index()
    val expireAt = long("expireAt")
    val role = short("role")


    override val primaryKey = PrimaryKey(inviteCode)
}