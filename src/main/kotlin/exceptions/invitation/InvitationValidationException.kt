package com.mapprjct.exceptions.invitation

import com.mapprjct.exceptions.BaseAppException
import com.mapprjct.model.dto.Role

sealed class InvitationValidationException : BaseAppException() {
    class InvalidUserRole(role : Role) : InvitationValidationException() {
        override val shortMessage: String = "Invalid user role"
        override val detailedMessage: String = "Cannot create invitation with role ${role.name}"
    }
}