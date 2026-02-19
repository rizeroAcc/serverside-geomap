package com.mapprjct.model.dto

import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.Username
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val phone : RussiaPhoneNumber,
    val username : Username,
    val avatarFilename : String? = null,
){
    companion object {
        fun create(phone : String, username : String,avatarFilename: String? = null) = User(
            phone = RussiaPhoneNumber(phone),
            username = Username(username),
            avatarFilename = avatarFilename
        )
    }
}