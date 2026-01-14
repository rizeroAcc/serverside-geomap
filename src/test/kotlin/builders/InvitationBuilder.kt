@file:OptIn(ExperimentalTime::class)

package com.mapprjct.builders

import com.mapprjct.model.Invitation
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.Role
import com.mapprjct.model.dto.User
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun createInvitation(block: InvitationBuilder.() -> Unit): Invitation {
    return InvitationBuilder().apply(block).build()
}

class InvitationBuilder {
    var inviterPhone : String? = null
    var inviteCode : UUID? = null
    var projectID : UUID? = null
    var expireAt : Long? = null
    var role : Role? = null

    fun build(): Invitation {
        return Invitation(
            inviterPhone = inviterPhone?: throw NullPointerException("Inviter phone not set"),
            inviteCode = inviteCode?: UUID.randomUUID(),
            projectID = projectID ?: throw NullPointerException("Project ID not set"),
            expireAt = expireAt ?: Clock.System.now().toEpochMilliseconds(),
            role = role ?: throw NullPointerException("Role not set"),
        )
    }

    fun fromInviter(user : User) {
        inviterPhone = user.phone
    }
    fun toProject(project: Project){
        projectID = UUID.fromString(project.projectID)
    }
    fun withRole(role : Role){
        this.role = role
    }
}