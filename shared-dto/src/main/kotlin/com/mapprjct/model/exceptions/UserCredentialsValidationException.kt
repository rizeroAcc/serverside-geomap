package com.mapprjct.model.exceptions

import java.lang.Exception

sealed class UserCredentialsValidationException : Exception(){
    class InvalidPasswordLength(val minLength : Int) : UserCredentialsValidationException()
    class InvalidPhone: UserCredentialsValidationException()
}