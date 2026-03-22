package com.mapprjct.model.dto

import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import kotlinx.serialization.Serializable

@Serializable
data class UserCredentialsDTO(
    val phone : RussiaPhoneNumber,
    val password : Password,
) {
    companion object {
        fun create(phone : String, password : String) = UserCredentialsDTO(
            phone = RussiaPhoneNumber(phone),
            password = Password(password)
        )
    }
}