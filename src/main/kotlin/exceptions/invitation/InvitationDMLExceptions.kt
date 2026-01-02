package com.mapprjct.exceptions.invitation

import com.mapprjct.exceptions.BaseAppException

sealed class InvitationDMLExceptions : BaseAppException() {
    class InvitationNotFoundException(code : String) : InvitationDMLExceptions() {
        override val shortMessage: String = "Invitation not found"
        override val detailedMessage: String = "Invitation with code $code not found"
    }
}