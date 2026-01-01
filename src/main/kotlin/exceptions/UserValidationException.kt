package com.mapprjct.exceptions

sealed class UserValidationException : BaseAppException() {
    class InvalidPhoneFormat() : UserValidationException() {
        override val shortMessage: String = "Invalid phone format"
        override val detailedMessage: String =
            """
                Incorrect user phone format. 
                It must start with '+7' or '8' and have valid length
                """.trimIndent()
    }

    class InvalidPasswordLength(minLength: Int) : UserValidationException() {
        override val shortMessage: String = "Password too short"
        override val detailedMessage: String = "Must have at least $minLength characters"
    }

    class InvalidUsername() : UserValidationException() {
        override val shortMessage: String = "Invalid username"
        override val detailedMessage: String = "Username must not be empty"
    }
}