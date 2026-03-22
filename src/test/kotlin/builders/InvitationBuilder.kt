@file:OptIn(ExperimentalTime::class)

package com.mapprjct.builders

import com.mapprjct.model.Invitation
import com.mapprjct.model.dto.ProjectDTO
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.utils.toUUID
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
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
            inviterPhone = inviterPhone?.let { RussiaPhoneNumber(it) }?: throw NullPointerException("Inviter phone not set"),
            inviteCode = inviteCode?: UUID.randomUUID(),
            projectID = projectID ?: throw NullPointerException("Project ID not set"),
            expireAt = expireAt ?: (Clock.System.now().toEpochMilliseconds() + 1.hours.inWholeMilliseconds),
            role = role ?: throw NullPointerException("Role not set"),
        )
    }

    fun fromInviter(inviterPhone : String) {
        this.inviterPhone = inviterPhone
    }
    fun toProject(projectID: StringUUID){
        this.projectID = projectID.toUUID()
    }
    fun toProject(project: ProjectDTO){
        this.projectID = project.projectID.toUUID()
    }
    fun withRole(role : Role){
        this.role = role
    }
}