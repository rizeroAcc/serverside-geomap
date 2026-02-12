package com.mapprjct.model.dto

import com.mapprjct.model.value.Password
import com.mapprjct.model.value.RussiaPhoneNumber
import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(
    val phone : RussiaPhoneNumber,
    val password : Password,
) {
    companion object {
        fun create(phone : String, password : String) = UserCredentials(
            phone = RussiaPhoneNumber(phone),
            password = Password(password)
        )
    }
}