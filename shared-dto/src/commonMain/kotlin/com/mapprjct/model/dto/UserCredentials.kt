package com.mapprjct.model.dto

import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
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