package com.mapprjct.exceptions.invitation

import com.mapprjct.exceptions.BaseAppException
import com.mapprjct.model.Role

sealed class InvitationValidationException : BaseAppException() {
    class InvalidUserRole(role : Role) : InvitationValidationException() {
        override val shortMessage: String = "Invalid user role"
        override val detailedMessage: String = "Cannot create invitation with role ${role.name}"
    }
    class NoPermissionToAddMembers(projectID : String) : InvitationValidationException() {
        override val shortMessage: String = "No permission to add members"
        override val detailedMessage: String = "No permission to add members to project: $projectID"
    }
    class UserNotStayInProject(projectID : String) : InvitationValidationException() {
        override val shortMessage: String = "User not stay in project"
        override val detailedMessage: String = "User not stay in project: $projectID"
    }
    class TooManyInvitationsPerUser : InvitationValidationException() {
        override val shortMessage: String = "Too many invitations per user"
        override val detailedMessage: String = "Try to register over five invitations. Remove old invitations and try again"
    }
}