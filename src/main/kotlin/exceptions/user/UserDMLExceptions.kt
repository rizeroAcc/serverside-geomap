package com.mapprjct.exceptions.user

import com.mapprjct.exceptions.BaseAppException

sealed class UserDMLExceptions : BaseAppException() {
    class UserAlreadyExistsException(phone : String) : UserDMLExceptions() {
        override val shortMessage: String = "User with $phone already exists"
        override val detailedMessage: String = "User with $phone already exists"
    }
    class UserNotFoundException(phone : String) : UserDMLExceptions() {
        override val shortMessage: String = "User with $phone not found"
        override val detailedMessage: String = "User with $phone not found"
    }
}