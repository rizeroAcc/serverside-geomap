package com.mapprjct.model.dto

import com.mapprjct.exceptions.user.UserValidationException
import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(
    val phone : String,
    val password : String,
) {

    fun validate(): Result<Unit> = when {
        !validatePhone(phone) ->
            Result.failure(UserValidationException.InvalidPhoneFormat())
        !validatePassword(password) ->
            Result.failure(UserValidationException.InvalidPasswordLength(8))
        else -> Result.success(Unit)
    }

    private fun validatePhone(phone : String) : Boolean{
        return if (phone.startsWith("8")) {
            phone.length == 11
        }else if(phone.startsWith("+7")) {
            phone.length == 12
        }else {
            false
        }
    }

    private fun validatePassword(password : String) : Boolean {
        return password.length >=8
    }
}