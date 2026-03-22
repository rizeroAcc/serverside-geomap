package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object InviteCodeTable : Table("invite_code") {
    val inviteCode = uuid("invite_code").uniqueIndex().clientDefault { UUID.randomUUID() }
    val projectID = reference("project_id", ProjectTable.id, onDelete = ReferenceOption.CASCADE)
    val inviterPhone = reference("inviter_phone",UserTable.phone, onDelete = ReferenceOption.CASCADE)
    val expireAt = long("expireAt").clientDefault { Clock.System.now().toEpochMilliseconds() }
    val role = short("role")

    override val primaryKey = PrimaryKey(inviteCode)
}